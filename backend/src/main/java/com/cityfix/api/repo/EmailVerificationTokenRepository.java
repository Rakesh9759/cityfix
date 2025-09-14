package com.cityfix.api.repo;

import com.cityfix.api.domain.auth.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
  Optional<EmailVerificationToken> findByToken(String token);
}