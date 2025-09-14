package com.cityfix.api.domain.issue;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_subscription",
       uniqueConstraints = @UniqueConstraint(name="uq_issue_subscription_issue_email",
         columnNames = {"issue_id","email"}))
public class IssueSubscription {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "issue_id", nullable = false)
  private Issue issue;

  @Column(nullable = false)
  private String email;

  @Column(name = "unsubscribe_token", nullable = false, unique = true)
  private String unsubscribeToken;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // getters/setters
  public UUID getId() { return id; }
  public Issue getIssue() { return issue; }
  public void setIssue(Issue issue) { this.issue = issue; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getUnsubscribeToken() { return unsubscribeToken; }
  public void setUnsubscribeToken(String unsubscribeToken) { this.unsubscribeToken = unsubscribeToken; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
}
