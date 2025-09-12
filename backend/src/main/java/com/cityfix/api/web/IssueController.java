package com.cityfix.api.web;

import com.cityfix.api.domain.issue.Category;
import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.domain.issue.IssueImage;
import com.cityfix.api.domain.issue.IssueStatus;
import com.cityfix.api.domain.issue.Severity;
import com.cityfix.api.repo.IssueImageRepository;
import com.cityfix.api.service.FileStorageService;
import com.cityfix.api.service.ImageIngestPublisher;
import com.cityfix.api.service.IssueService;
import com.cityfix.api.web.dto.ImageDtos.ImageResponse;
import com.cityfix.api.web.dto.ImageDtos.PresignResponse;
import com.cityfix.api.web.dto.IssueDtos.CreateIssueRequest;
import com.cityfix.api.web.dto.IssueDtos.IssueResponse;
import com.cityfix.api.web.hateoas.IssueModelAssembler;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/v1")
public class IssueController {

  private final IssueService service;
  private final IssueModelAssembler assembler;
  private final IssueImageRepository imageRepo;
  private final FileStorageService storage;
  private final ImageIngestPublisher ingestPublisher;

  public IssueController(IssueService service,
                         IssueModelAssembler assembler,
                         IssueImageRepository imageRepo,
                         FileStorageService storage,
                         ImageIngestPublisher ingestPublisher) {
    this.service = service;
    this.assembler = assembler;
    this.imageRepo = imageRepo;
    this.storage = storage;
    this.ingestPublisher = ingestPublisher;
  }

  // ---- Create
  @PostMapping("/issues")
  public ResponseEntity<EntityModel<IssueResponse>> create(@Valid @RequestBody CreateIssueRequest req) {
    Issue i = new Issue();
    i.setTitle(req.title());
    i.setDescription(req.description());
    i.setCategory(req.category());
    i.setSeverity(req.severity());
    i.setLat(req.lat());
    i.setLon(req.lon());
    i.setWardCode(req.wardCode());
    i.setObservedAt(req.observedAt());
    i.setLocationSource("user");

    Issue saved = service.create(i);
    EntityModel<IssueResponse> model = assembler.toModel(saved);
    URI self = linkTo(methodOn(IssueController.class).get(saved.getId().toString())).toUri();
    return ResponseEntity.created(self).body(model);
  }

  // ---- Get one
  @GetMapping("/issues/{id}")
  public ResponseEntity<EntityModel<IssueResponse>> get(@PathVariable String id) {
    var opt = service.get(UUID.fromString(id));
    if (opt.isEmpty()) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(assembler.toModel(opt.get()));
  }

  // ---- List/search (filters + optional bbox)
  @GetMapping("/issues")
  public ResponseEntity<PagedModel<EntityModel<IssueResponse>>> list(
      @RequestParam(required = false) IssueStatus status,
      @RequestParam(required = false) Category category,
      @RequestParam(required = false) String ward,
      @RequestParam(required = false) Double minLon,
      @RequestParam(required = false) Double minLat,
      @RequestParam(required = false) Double maxLon,
      @RequestParam(required = false) Double maxLat,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      PagedResourcesAssembler<Issue> pagedAssembler
  ) {
    Page<Issue> p = service.search(status, category, ward, minLon, minLat, maxLon, maxLat, page, size);
    PagedModel<EntityModel<IssueResponse>> model = pagedAssembler.toModel(p, assembler);
    return ResponseEntity.ok(model);
  }

  // ======================
  // Images (dev "presign" + upload + list + raw)
  // ======================

  // 1) "Presign" (dev): tell client the upload endpoint (build path directly to avoid checked-exception method refs)
  @PostMapping("/issues/{id}/images/presign")
  public ResponseEntity<PresignResponse> presignImages(@PathVariable String id,
                                                       @RequestBody(required = false) Object body) {
    String uploadUrl = linkTo(IssueController.class)
        .slash("issues").slash(id).slash("images").slash("upload")
        .toUri().toString();

    return ResponseEntity.ok(new PresignResponse(
        uploadUrl,
        10 * 1024 * 1024L,
        new String[]{"image/jpeg", "image/png"}
    ));
  }

  // 2) Upload (multipart) -> save file -> DB -> enqueue image.ingest
  @PostMapping(value = "/issues/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadImage(@PathVariable String id,
                                       @RequestPart("file") MultipartFile file) throws Exception {
    var issue = service.get(UUID.fromString(id)).orElse(null);
    if (issue == null) return ResponseEntity.notFound().build();
    if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "file is required"));

    var saved = storage.saveRaw(issue.getId(), file.getOriginalFilename(), file.getContentType(), file.getInputStream());

    var img = new IssueImage();
    img.setIssue(issue);
    img.setStorageKey(saved.key());
    img.setContentType(saved.contentType());
    img.setWidth(saved.width());
    img.setHeight(saved.height());
    img = imageRepo.save(img);

    // Publish for EXIF/thumbnail worker (Step 8)
    ingestPublisher.publish(issue.getId(), saved.key());

    var res = new ImageResponse(img.getId(), img.getContentType(), img.getWidth(), img.getHeight(), img.getStorageKey());
    var self = linkTo(methodOn(IssueController.class).getImage(id, img.getId().toString())).toUri();
    return ResponseEntity.created(self).body(res);
  }

  // 3) List images for an issue
  @GetMapping("/issues/{id}/images")
  public ResponseEntity<List<ImageResponse>> listImages(@PathVariable String id) {
    var issueId = UUID.fromString(id);
    var list = imageRepo.findAllByIssue_IdOrderByCreatedAtAsc(issueId).stream()
        .map(img -> new ImageResponse(img.getId(), img.getContentType(), img.getWidth(), img.getHeight(), img.getStorageKey()))
        .toList();
    return ResponseEntity.ok(list);
  }

  // 4) Get one image (metadata) with links
  @GetMapping("/issues/{id}/images/{imageId}")
  public ResponseEntity<EntityModel<ImageResponse>> getImage(@PathVariable String id, @PathVariable String imageId) {
    var issueId = UUID.fromString(id);
    var imgId = UUID.fromString(imageId);
    var img = imageRepo.findById(imgId).orElse(null);
    if (img == null || !img.getIssue().getId().equals(issueId)) return ResponseEntity.notFound().build();

    var dto = new ImageResponse(img.getId(), img.getContentType(), img.getWidth(), img.getHeight(), img.getStorageKey());
    var model = EntityModel.of(dto);
    model.add(linkTo(methodOn(IssueController.class).getImage(id, imageId)).withSelfRel());

    // Build raw link directly to avoid checked-exception signature
    String rawHref = linkTo(IssueController.class)
        .slash("issues").slash(id).slash("images").slash(imageId).slash("raw")
        .toUri().toString();
    model.add(Link.of(rawHref, "raw"));

    return ResponseEntity.ok(model);
  }

  // 5) Stream raw bytes
  @GetMapping("/issues/{id}/images/{imageId}/raw")
  public ResponseEntity<?> rawImage(@PathVariable String id, @PathVariable String imageId) throws Exception {
    var issueId = UUID.fromString(id);
    var imgId = UUID.fromString(imageId);
    var img = imageRepo.findById(imgId).orElse(null);
    if (img == null || !img.getIssue().getId().equals(issueId)) return ResponseEntity.notFound().build();

    var path = storage.resolve(img.getStorageKey());
    if (!Files.exists(path)) return ResponseEntity.notFound().build();

    var is = Files.newInputStream(path);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
        .contentType(MediaType.parseMediaType(img.getContentType()))
        .body(new InputStreamResource(is));
  }

  // ======================
  // Stubs (to be implemented in later steps)
  // ======================

  @PostMapping("/issues/{id}/subscribe")
  public ResponseEntity<?> subscribe(@PathVariable String id) {
    return ResponseEntity.status(501).body(Map.of("message", "subscribe not implemented yet"));
  }

  @PostMapping("/issues/{id}/transition")
  public ResponseEntity<?> transition(@PathVariable String id, @RequestBody(required = false) Object body) {
    return ResponseEntity.status(501).body(Map.of("message", "transition not implemented yet"));
  }
}
