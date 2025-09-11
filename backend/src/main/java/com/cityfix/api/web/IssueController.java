package com.cityfix.api.web;

import com.cityfix.api.domain.issue.*;
import com.cityfix.api.service.IssueService;
import com.cityfix.api.web.dto.IssueDtos.CreateIssueRequest;
import com.cityfix.api.web.dto.IssueDtos.IssueResponse;
import com.cityfix.api.web.hateoas.IssueModelAssembler;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/v1")
public class IssueController {

  private final IssueService service;
  private final IssueModelAssembler assembler;

  public IssueController(IssueService service, IssueModelAssembler assembler) {
    this.service = service; this.assembler = assembler;
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
    return service.get(UUID.fromString(id))
        .map(assembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
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
    PagedModel<EntityModel<IssueResponse>> paged = pagedAssembler.toModel(
        p,
        assembler,
        linkTo(methodOn(IssueController.class)
            .list(status, category, ward, minLon, minLat, maxLon, maxLat, page, size, null))
            .withSelfRel()
    );
    return ResponseEntity.ok(paged);
  }

  // ---- Stubs for HATEOAS links (501 for now; will implement later)
  @PostMapping("/issues/{id}/images/presign")
  public ResponseEntity<?> presignImages(@PathVariable String id, @RequestBody(required = false) Object body) {
    return ResponseEntity.status(501).body(java.util.Map.of("message","presign not implemented yet"));
  }

  @PostMapping("/issues/{id}/subscribe")
  public ResponseEntity<?> subscribe(@PathVariable String id) {
    return ResponseEntity.status(501).body(java.util.Map.of("message","subscribe not implemented yet"));
  }

  @PostMapping("/issues/{id}/transition")
  public ResponseEntity<?> transition(@PathVariable String id, @RequestBody(required = false) Object body) {
    return ResponseEntity.status(501).body(java.util.Map.of("message","transition not implemented yet"));
  }
}
