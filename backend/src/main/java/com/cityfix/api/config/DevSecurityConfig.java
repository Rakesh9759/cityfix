package com.cityfix.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev") // active only when SPRING_PROFILES_ACTIVE=dev
public class DevSecurityConfig {
  private static final Logger log = LoggerFactory.getLogger(DevSecurityConfig.class);

  @PostConstruct
  void logActive() {
    log.info("DevSecurityConfig ACTIVE: permitting ALL requests");
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE) // ensure this chain wins
  public SecurityFilterChain devAll(HttpSecurity http) throws Exception {
    return http
        // catch EVERYTHING in dev, not just /v1/**
        .securityMatcher("/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .httpBasic(b -> b.disable())
        .formLogin(f -> f.disable())
        .logout(l -> l.disable())
        .build();
  }
}
