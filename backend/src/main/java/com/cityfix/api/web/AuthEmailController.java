package com.cityfix.api.web;

import com.cityfix.api.domain.auth.EmailVerificationToken;
import com.cityfix.api.domain.auth.VerifiedEmail;
import com.cityfix.api.repo.EmailVerificationTokenRepository;
import com.cityfix.api.repo.VerifiedEmailRepository;
import com.cityfix.api.service.SesEmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/email")
public class AuthEmailController {

  private final EmailVerificationTokenRepository tokens;
  private final VerifiedEmailRepository verified;
  private final SesEmailService mail;

  public AuthEmailController(EmailVerificationTokenRepository tokens,
                             VerifiedEmailRepository verified,
                             SesEmailService mail) {
    this.tokens = tokens;
    this.verified = verified;
    this.mail = mail;
  }

  public record VerifyRequest(String email) {}

  @PostMapping("/verify/request")
  public ResponseEntity<?> request(@RequestBody VerifyRequest req) {
    if (req == null || !StringUtils.hasText(req.email())) {
      return ResponseEntity.badRequest().body(Map.of("error","email is required"));
    }
    // already verified? idempotent
    if (verified.existsByEmailIgnoreCase(req.email())) return ResponseEntity.accepted().build();

    var tok = new EmailVerificationToken();
    tok.setEmail(req.email());
    tok.setToken(UUID.randomUUID().toString());
    tok.setExpiresAt(OffsetDateTime.now().plusHours(2));
    tokens.save(tok);

    String link = "http://localhost:8080/auth/email/verify?token=" + tok.getToken();
    mail.sendVerifyLink(req.email(), link);

    return ResponseEntity.accepted().build();
  }

  @GetMapping("/verify")
  public ResponseEntity<?> verify(@RequestParam String token) {
    var opt = tokens.findByToken(token);
    if (opt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","invalid token"));

    var t = opt.get();
    if (t.getUsedAt() != null) return ResponseEntity.badRequest().body(Map.of("error","token already used"));
    if (t.getExpiresAt().isBefore(OffsetDateTime.now())) return ResponseEntity.badRequest().body(Map.of("error","token expired"));

    // mark verified
    if (!verified.existsByEmailIgnoreCase(t.getEmail())) {
      var ve = new VerifiedEmail();
      ve.setEmail(t.getEmail());
      verified.save(ve);
    }
    t.setUsedAt(OffsetDateTime.now());
    tokens.save(t);

    return ResponseEntity.ok(Map.of("verified", true, "email", t.getEmail()));
  }
}