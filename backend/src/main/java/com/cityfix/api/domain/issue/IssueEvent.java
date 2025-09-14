package com.cityfix.api.domain.issue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "issue_event")
public class IssueEvent {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "issue_id")
  private Issue issue;

  @Column(nullable = false)
  private String type; // e.g. "issue.status.changed"

  // Store arbitrary JSON payload in Postgres jsonb
  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "actor_user_id")
  private UUID actorUserId; // optional

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Issue getIssue() { return issue; }
  public void setIssue(Issue issue) { this.issue = issue; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public Map<String, Object> getPayload() { return payload; }
  public void setPayload(Map<String, Object> payload) { this.payload = payload; }

  public UUID getActorUserId() { return actorUserId; }
  public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
}
