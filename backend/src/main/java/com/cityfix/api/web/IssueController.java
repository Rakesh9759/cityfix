package com.cityfix.api.web;

import com.cityfix.api.domain.issue.Category;
import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.domain.issue.IssueImage;
import com.cityfix.api.domain.issue.IssueStatus;
import com.cityfix.api.domain.issue.Severity;
import com.cityfix.api.repo.IssueEventRepository;
import com.cityfix.api.repo.IssueImageRepository;
import com.cityfix.api.repo.IssueRepository;
import com.cityfix.api.service.FileStorageService;
import com.cityfix.api.service.ImageIngestPublisher;
import com.cityfix.api.web.dto.IssueCreate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.io.InputStream;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/v1/issues")
public class IssueController {

  private static final Logger log = LoggerFactory.getLogger(IssueController.class);

  private final IssueRepository issueRepository;
  private final IssueImageRepository imageRepository;
  private final IssueEventRepository eventRepository;
  private final com.cityfix.api.web.hateoas.IssueModelAssembler assembler;
  private final FileStorageService storage;
  private final ImageIngestPublisher imagePublisher;
  private final ObjectMapper objectMapper;

  public IssueController(
      IssueRepository issueRepository,
      IssueImageRepository imageRepository,
      IssueEventRepository eventRepository,
      com.cityfix.api.web.hateoas.IssueModelAssembler assembler,
      FileStorageService storage,
      ImageIngestPublisher imagePublisher,
      ObjectMapper objectMapper
  ) {
    this.issueRepository = issueRepository;
    this.imageRepository = imageRepository;
    this.eventRepository = eventRepository;
    this.assembler = assembler;
    this.storage = storage;
    this.imagePublisher = imagePublisher;
    this.objectMapper = objectMapper;
  }

  // -------- List ----------
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> list(Pageable pageable, PagedResourcesAssembler<Issue> pagedAssembler) {
    var page = issueRepository.findAll(pageable);
    PagedModel<EntityModel<?>> model =
        pagedAssembler.toModel(page, issue -> (EntityModel<?>) assembler.toModel(issue));
    return ResponseEntity.ok().body(model);
  }

  // -------- Get one (assembler expects name 'get') ----------
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> get(@PathVariable String id) {
    var uuid = UUID.fromString(id);
    return issueRepository.findById(uuid)
        .map(i -> (EntityModel<?>) assembler.toModel(i))
        .map(body -> ResponseEntity.ok().body(body))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  // -------- Create ----------
  @PostMapping(
      consumes = { MediaType.APPLICATION_JSON_VALUE, "application/*+json" },
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<?> create(@RequestBody byte[] raw) {
    final String body = new String(raw, StandardCharsets.UTF_8);
    log.info("POST /v1/issues raw bytes = {}, body='{}'", raw.length, preview(body));

    final IssueCreate req;
    try {
      req = objectMapper.readValue(body, IssueCreate.class);
    } catch (JsonProcessingException jpe) {
      int idx = jpe.getLocation() != null ? (int) jpe.getLocation().getCharOffset() : -1;
      String around = snippet(body, Math.max(0, idx - 24), Math.min(body.length(), idx + 24));
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid JSON near index " + idx + ": …" + printable(around) + "…",
          jpe
      );
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON: " + e.getMessage(), e);
    }

    if (!StringUtils.hasText(req.title()))    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
    if (!StringUtils.hasText(req.category())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
    if (!StringUtils.hasText(req.severity())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "severity is required");

    // Fuzzy, case-insensitive enum parsing with friendly errors
    final Category category;
    try {
      category = parseEnum(Category.class, req.category());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Unknown category: " + req.category() + " (allowed: " + String.join(", ", enumNames(Category.class)) + ")"
      );
    }

    final Severity severity;
    try {
      severity = parseEnum(Severity.class, req.severity());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Unknown severity: " + req.severity() + " (allowed: " + String.join(", ", enumNames(Severity.class)) + ")"
      );
    }

    Issue issue = new Issue();
    issue.setTitle(req.title());
    issue.setDescription(req.description());
    issue.setCategory(category);
    issue.setSeverity(severity);
    issue.setLat(req.lat());
    issue.setLon(req.lon());

    Issue saved = issueRepository.save(issue);
    var bodyModel = (EntityModel<?>) assembler.toModel(saved);
    URI self = linkTo(methodOn(IssueController.class).get(saved.getId().toString())).toUri();
    return ResponseEntity.created(self).body(bodyModel);
  }

  // -------- Allowed transitions ----------
  @GetMapping(value = "/{id}/transitions", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> transitions(@PathVariable String id) {
    var issue = issueRepository.findById(UUID.fromString(id))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    List<IssueStatus> allowed;
    switch (issue.getStatus()) {
      case NEW -> allowed = List.of(IssueStatus.TRIAGED, IssueStatus.CLOSED);
      case TRIAGED -> allowed = List.of(IssueStatus.IN_PROGRESS, IssueStatus.CLOSED);
      case IN_PROGRESS -> allowed = List.of(IssueStatus.RESOLVED);
      case RESOLVED -> allowed = List.of(IssueStatus.CLOSED);
      default -> allowed = List.of();
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", issue.getId().toString());
    body.put("from", issue.getStatus().name());
    body.put("allowed", allowed.stream().map(Enum::name).toList());
    return ResponseEntity.ok(body);
  }

  // -------- Transition ----------
  public static record TransitionRequest(String to, String note) {}

  @PostMapping(value = "/{id}/transition", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> transition(@PathVariable String id, @RequestBody TransitionRequest req) {
    var issue = issueRepository.findById(UUID.fromString(id))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!StringUtils.hasText(req.to())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`to` status is required");
    }

    IssueStatus to;
    try {
      to = IssueStatus.valueOf(normalizeEnum(req.to()));
    } catch (IllegalArgumentException iae) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + req.to());
    }

    var allowedResp = transitions(id).getBody();
    @SuppressWarnings("unchecked")
    var allowed = allowedResp != null ? (List<String>) allowedResp.get("allowed") : List.<String>of();
    if (!allowed.contains(to.name())) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
          "Transition " + issue.getStatus() + " -> " + to + " not allowed");
    }

    issue.setStatus(to);
    Issue saved = issueRepository.save(issue);
    return ResponseEntity.ok().body((EntityModel<?>) assembler.toModel(saved));
  }

  // -------- Images: presign link (assembler expects name 'presignImages') ----------
  @GetMapping(value = "/{id}/images/presign", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, String>> presignImages(@PathVariable String id,
                                                           @RequestParam(required = false) String unused) {
    String uploadHref = linkTo(methodOn(IssueController.class).uploadImage(id, null)).toUri().toString();
    return ResponseEntity.ok(Map.of("uploadUrl", uploadHref));
  }

  // -------- Images: multipart upload ----------
  @PostMapping(value = "/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> uploadImage(@PathVariable String id,
                                                         @RequestPart("file") MultipartFile file) {
    try {
      var issue = issueRepository.findById(UUID.fromString(id))
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

      if (file == null || file.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
      }

      var saved = storage.saveRaw(issue.getId(),
          file.getOriginalFilename(),
          file.getContentType(),
          file.getInputStream());

      IssueImage img = new IssueImage();
      img.setIssue(issue);
      img.setContentType(saved.contentType());

      // Save key and try to capture width/height
      setImageKeyViaReflection(img, saved.key());

      try (InputStream in = Files.newInputStream(storage.resolve(saved.key()))) {
        var buf = ImageIO.read(in);
        if (buf != null) {
          img.setWidth(buf.getWidth());
          img.setHeight(buf.getHeight());
        }
      } catch (Exception ignore) {
        // non-fatal: keep going even if we can't read dimensions
      }

      imageRepository.save(img);

      tryPublishIngest(issue.getId(), img.getId(), saved.key());

      var rawHref = linkTo(methodOn(IssueController.class).rawImage(id, img.getId().toString())).toUri().toString();
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("imageId", img.getId().toString());
      body.put("raw", rawHref);
      return ResponseEntity.status(HttpStatus.CREATED).body(body);
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }
  }

  // -------- Images: list ----------
  @GetMapping(value = "/{id}/images", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<Map<String, Object>>> listImages(@PathVariable String id) {
    var issueId = UUID.fromString(id);

    // Use the repo method you currently have
    var images = imageRepository.findAllByIssue_IdOrderByCreatedAtAsc(issueId);

    List<Map<String, Object>> rows = images.stream().map(ii -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", ii.getId().toString());

      // only include when present to avoid nulls
      if (StringUtils.hasText(ii.getContentType())) m.put("contentType", ii.getContentType());
      if (ii.getWidth() != null)  m.put("width", ii.getWidth());
      if (ii.getHeight() != null) m.put("height", ii.getHeight());

      m.put("raw", linkTo(methodOn(IssueController.class)
          .rawImage(id, ii.getId().toString()))
          .toUri().toString());
      return m;
    }).toList();

    return ResponseEntity.ok(rows);
  }

  // -------- Images: raw fetch ----------
  @GetMapping(value = "/{id}/images/{imageId}/raw")
  public ResponseEntity<Resource> rawImage(@PathVariable String id, @PathVariable String imageId) {
    var issueId = UUID.fromString(id);
    var imgId = UUID.fromString(imageId);

    var img = imageRepository.findById(imgId)
        .filter(ii -> ii.getIssue().getId().equals(issueId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    try {
      String key = getImageKeyViaReflection(img);
      if (!StringUtils.hasText(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image key missing");

      Path path = storage.resolve(key);
      if (!Files.exists(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

      String contentType = img.getContentType();
      if (!StringUtils.hasText(contentType)) {
        contentType = Files.probeContentType(path);
        if (!StringUtils.hasText(contentType)) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
      }
      String filename = path.getFileName().toString();
      var headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(contentType));
      headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
      return new ResponseEntity<>(new FileSystemResource(path), headers, HttpStatus.OK);
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }
  }

  // -------- Subscribe stub ----------
  @PostMapping("/{id}/subscribe")
  public ResponseEntity<Void> subscribe(@PathVariable String id) {
    return ResponseEntity.accepted().build();
  }

  // ===== Helpers (enum normalization + reflective key handling + publisher) =====

  private static String normalizeEnum(String s) {
    return s.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
    String norm = normalizeEnum(raw);

    // 1) exact match after normalization
    for (E e : type.getEnumConstants()) {
      if (e.name().equals(norm)) return e;
    }
    // 2) fuzzy containment
    for (E e : type.getEnumConstants()) {
      String en = e.name().replaceAll("[^A-Z0-9]", "");
      String rn = norm.replaceAll("[^A-Z0-9]", "");
      if (en.equals(rn) || en.contains(rn) || rn.contains(en)) return e;
    }
    throw new IllegalArgumentException("No enum match");
  }

  private static <E extends Enum<E>> String[] enumNames(Class<E> type) {
    return Stream.of(type.getEnumConstants()).map(Enum::name).toArray(String[]::new);
  }

  private static final List<String> KEY_FIELDS = List.of("s3Key", "storageKey", "key", "path");

  private static void setImageKeyViaReflection(IssueImage img, String key) {
    for (String f : KEY_FIELDS) {
      try {
        Field field = img.getClass().getDeclaredField(f);
        field.setAccessible(true);
        field.set(img, key);
        return;
      } catch (NoSuchFieldException ignored) {
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalStateException("IssueImage has no known key field (tried: " + KEY_FIELDS + ")");
  }

  private static String getImageKeyViaReflection(IssueImage img) {
    for (String f : KEY_FIELDS) {
      try {
        Field field = img.getClass().getDeclaredField(f);
        field.setAccessible(true);
        Object val = field.get(img);
        if (val instanceof String s && StringUtils.hasText(s)) return s;
      } catch (NoSuchFieldException ignored) {
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  private void tryPublishIngest(UUID issueId, UUID imageId, String key) {
    try {
      var m = imagePublisher.getClass().getMethod("publishIngest", UUID.class, UUID.class, String.class);
      m.invoke(imagePublisher, issueId, imageId, key);
      return;
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      var m = imagePublisher.getClass().getMethod("publishIngest", UUID.class, UUID.class);
      m.invoke(imagePublisher, issueId, imageId);
      return;
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      var m = imagePublisher.getClass().getMethod("publish", String.class);
      m.invoke(imagePublisher, key);
    } catch (NoSuchMethodException ignored) {
      // no-op
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // Small helpers for logging/diagnostics
  private static String preview(String s) {
    String t = s.replace("\n", "\\n").replace("\r", "\\r");
    return t.length() <= 200 ? t : t.substring(0, 200) + "…(" + t.length() + " chars)";
  }
  private static String snippet(String s, int from, int to) {
    return s.substring(from, to);
  }
  private static String printable(String s) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < 32 || c == 127) b.append(String.format("\\u%04x", (int) c));
      else b.append(c);
    }
    return b.toString();
  }
}
