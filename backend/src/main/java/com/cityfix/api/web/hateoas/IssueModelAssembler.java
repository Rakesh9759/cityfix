// package com.cityfix.api.web.hateoas;

// import com.cityfix.api.domain.issue.Issue;
// import com.cityfix.api.web.IssueController;
// import com.cityfix.api.web.dto.IssueDtos.IssueResponse;
// import org.springframework.hateoas.EntityModel;
// import org.springframework.hateoas.Link;
// import org.springframework.hateoas.server.RepresentationModelAssembler;
// import org.springframework.stereotype.Component;

// import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
// import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

// @Component
// public class IssueModelAssembler implements RepresentationModelAssembler<Issue, EntityModel<IssueResponse>> {

//   @Override
//   public EntityModel<IssueResponse> toModel(Issue issue) {
//     var dto = new IssueResponse(
//         issue.getId(),
//         issue.getTitle(),
//         issue.getDescription(),
//         issue.getCategory(),
//         issue.getSeverity(),
//         issue.getStatus(),
//         issue.getLat(),
//         issue.getLon(),
//         issue.getWardCode(),
//         issue.getObservedAt(),
//         issue.getCreatedAt(),
//         issue.getUpdatedAt()
//     );

//     var model = EntityModel.of(dto);
//     var idStr = issue.getId().toString();

//     // self
//     model.add(
//         linkTo(methodOn(IssueController.class).get(idStr)).withSelfRel()
//     );

//     // presign (dev flow)
//     model.add(
//         Link.of(
//             linkTo(methodOn(IssueController.class).presignImages(idStr, null))
//                 .toUri().toString(),
//             "images:presign"
//         )
//     );

//     // upload (build via controller method — NO manual ".slash(\"issues\")")
//     model.add(
//         linkTo(methodOn(IssueController.class).uploadImage(idStr, null))
//             .withRel("images:upload")
//     );

//     // subscribe
//     model.add(
//         Link.of(
//             linkTo(methodOn(IssueController.class).subscribe(idStr))
//                 .toUri().toString(),
//             "subscribe"
//         )
//     );

//     // transition
//     model.add(
//         Link.of(
//             linkTo(methodOn(IssueController.class).transition(idStr, null))
//                 .toUri().toString(),
//             "transition"
//         )
//     );

//     return model;
//   }
// }

package com.cityfix.api.web.hateoas;

import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.web.IssueController;
import com.cityfix.api.web.dto.IssueDtos.IssueResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class IssueModelAssembler implements RepresentationModelAssembler<Issue, EntityModel<IssueResponse>> {

  @Override
  public EntityModel<IssueResponse> toModel(Issue issue) {
    var dto = new IssueResponse(
        issue.getId(),
        issue.getTitle(),
        issue.getDescription(),
        issue.getCategory(),
        issue.getSeverity(),
        issue.getStatus(),
        issue.getLat(),
        issue.getLon(),
        issue.getWardCode(),
        issue.getObservedAt(),
        issue.getCreatedAt(),
        issue.getUpdatedAt()
    );

    var model = EntityModel.of(dto);
    var idStr = issue.getId().toString();

    // self
    model.add(linkTo(methodOn(IssueController.class).get(idStr)).withSelfRel());

    // presign (dev flow)
    model.add(linkTo(methodOn(IssueController.class).presignImages(idStr, null))
        .withRel("images:presign"));

    // upload (build via controller method — no manual ".slash(\"issues\")")
    model.add(linkTo(methodOn(IssueController.class).uploadImage(idStr, null))
        .withRel("images:upload"));

    // subscribe
    model.add(linkTo(methodOn(IssueController.class).subscribe(idStr))
        .withRel("subscribe"));

    // transition (POST)
    model.add(linkTo(methodOn(IssueController.class).transition(idStr, null))
        .withRel("transition"));

    // allowed transitions (GET)
    model.add(linkTo(methodOn(IssueController.class).transitions(idStr))
        .withRel("transitions"));

    return model;
  }
}
