package com.cityfix.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!dev") // NEVER loads in dev
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/v1/**", "/actuator/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/v1/**").permitAll()
            .requestMatchers(HttpMethod.HEAD, "/v1/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/**").authenticated()
            .requestMatchers(HttpMethod.PUT, "/v1/**").authenticated()
            .requestMatchers(HttpMethod.PATCH, "/v1/**").authenticated()
            .requestMatchers(HttpMethod.DELETE, "/v1/**").authenticated()
            .anyRequest().denyAll()
        )
        .httpBasic(b -> b.disable())
        .formLogin(f -> f.disable())
        .logout(l -> l.disable())
        .build();
  }
}
