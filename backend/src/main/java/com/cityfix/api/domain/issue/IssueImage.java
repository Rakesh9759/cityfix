package com.cityfix.api.domain.issue;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "issue_image")
public class IssueImage {
  @Id @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "issue_id", nullable = false)
  private Issue issue;

  @Column(name = "s3_key", nullable = false)
  private String storageKey; // reusing column s3_key for local path/key

  @Column(name = "content_type", nullable = false)
  private String contentType;

  private Integer width;
  private Integer height;

  @Column(name = "is_thumb", nullable = false)
  private boolean thumb = false;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // getters/setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Issue getIssue() { return issue; }
  public void setIssue(Issue issue) { this.issue = issue; }
  public String getStorageKey() { return storageKey; }
  public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public Integer getWidth() { return width; }
  public void setWidth(Integer width) { this.width = width; }
  public Integer getHeight() { return height; }
  public void setHeight(Integer height) { this.height = height; }
  public boolean isThumb() { return thumb; }
  public void setThumb(boolean thumb) { this.thumb = thumb; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
}