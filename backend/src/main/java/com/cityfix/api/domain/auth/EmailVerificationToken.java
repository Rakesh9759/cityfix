package com.cityfix.api.domain.auth;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verification_token",
  uniqueConstraints = @UniqueConstraint(name="uq_email_verify_token_token", columnNames = "token"))
public class EmailVerificationToken {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "used_at")
  private OffsetDateTime usedAt;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  // getters/setters
  public UUID getId() { return id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public OffsetDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
  public OffsetDateTime getUsedAt() { return usedAt; }
  public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
}
