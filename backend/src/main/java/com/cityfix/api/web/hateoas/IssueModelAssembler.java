package com.cityfix.api.web.hateoas;

import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.web.IssueController;
import com.cityfix.api.web.dto.IssueDtos.IssueResponse;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class IssueModelAssembler implements RepresentationModelAssembler<Issue, EntityModel<IssueResponse>> {

  @Override
  public EntityModel<IssueResponse> toModel(Issue issue) {
    var dto = new IssueResponse(
        issue.getId(), issue.getTitle(), issue.getDescription(),
        issue.getCategory(), issue.getSeverity(), issue.getStatus(),
        issue.getLat(), issue.getLon(), issue.getWardCode(),
        issue.getObservedAt(), issue.getCreatedAt(), issue.getUpdatedAt()
    );

    var model = EntityModel.of(dto);
    model.add(linkTo(methodOn(IssueController.class).get(issue.getId().toString())).withSelfRel());
    model.add(Link.of(linkTo(methodOn(IssueController.class).presignImages(issue.getId().toString(), null)).toUri().toString(), "images:presign"));
    model.add(Link.of(linkTo(methodOn(IssueController.class).subscribe(issue.getId().toString())).toUri().toString(), "subscribe"));
    model.add(Link.of(linkTo(methodOn(IssueController.class).transition(issue.getId().toString(), null)).toUri().toString(), "transition"));
    return model;
  }
}