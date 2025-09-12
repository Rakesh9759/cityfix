package com.cityfix.api.domain.issue;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "issue_event")
public class IssueEvent {
  @Id @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "issue_id", nullable = false)
  private Issue issue;

  @Column(name = "actor_user_id", columnDefinition = "uuid")
  private UUID actorUserId;

  @Column(nullable = false)
  private String type;              // created, exif_location_detected, thumbnail_generated, etc.

  @Column(columnDefinition = "jsonb")
  private String payload;           // small JSON blob as string

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // getters/setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Issue getIssue() { return issue; }
  public void setIssue(Issue issue) { this.issue = issue; }
  public UUID getActorUserId() { return actorUserId; }
  public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
}