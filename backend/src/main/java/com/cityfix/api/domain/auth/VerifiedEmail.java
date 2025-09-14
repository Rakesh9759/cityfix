package com.cityfix.api.domain.auth;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verified_email", uniqueConstraints =
  @UniqueConstraint(name="uq_verified_email_email", columnNames = "email"))
public class VerifiedEmail {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();
EmailVerificationToken.java
  @Column(nullable = false)
  private String email;

  @Column(name = "verified_at", nullable = false)
  private OffsetDateTime verifiedAt = OffsetDateTime.now();

  public UUID getId() { return id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public OffsetDateTime getVerifiedAt() { return verifiedAt; }
  public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
