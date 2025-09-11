package com.cityfix.api.web.dto;

import com.cityfix.api.domain.issue.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public class IssueDtos {

  public record CreateIssueRequest(
      @NotBlank @Size(max=200) String title,
      @Size(max=10_000) String description,
      @NotNull Category category,
      @NotNull Severity severity,
      @NotNull @DecimalMin(value="-90") @DecimalMax(value="90") Double lat,
      @NotNull @DecimalMin(value="-180") @DecimalMax(value="180") Double lon,
      String wardCode,
      OffsetDateTime observedAt
  ) {}

  public record IssueResponse(
      UUID id,
      String title,
      String description,
      Category category,
      Severity severity,
      IssueStatus status,
      Double lat,
      Double lon,
      String wardCode,
      OffsetDateTime observedAt,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {}
}