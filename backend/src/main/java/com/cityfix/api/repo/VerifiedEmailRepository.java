package com.cityfix.api.repo;

import com.cityfix.api.domain.auth.VerifiedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerifiedEmailRepository extends JpaRepository<VerifiedEmail, UUID> {
  Optional<VerifiedEmail> findByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCase(String email);
}