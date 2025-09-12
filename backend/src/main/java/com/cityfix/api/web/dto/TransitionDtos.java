package com.cityfix.api.web.dto;

import com.cityfix.api.domain.issue.IssueStatus;
import jakarta.validation.constraints.NotNull;

public class TransitionDtos {

  public record TransitionRequest(
      @NotNull IssueStatus to,
      String note
  ) {}

  public record AllowedResponse(IssueStatus[] allowed) {}
}