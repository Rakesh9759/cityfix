// package com.cityfix.api.web.dto;

// import com.cityfix.api.domain.issue.Category;
// import com.cityfix.api.domain.issue.Severity;
// import jakarta.validation.constraints.*;

// public record IssueCreate(
//     @NotBlank String title,
//     String description,
//     @NotNull Category category,
//     @NotNull Severity severity,
//     @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double lat,
//     @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double lon
//     // If you want: OffsetDateTime observedAt
// ) {}
// src/main/java/com/cityfix/api/web/dto/IssueCreate.java
package com.cityfix.api.web.dto;

public record IssueCreate(
  String title,
  String description,
  String category,
  String severity,
  Double lat,
  Double lon
  // Optional: OffsetDateTime observedAt
) {}
