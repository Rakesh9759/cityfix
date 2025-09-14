package com.cityfix.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class HttpLoggingConfig {
  @Bean
  CommonsRequestLoggingFilter requestLoggingFilter() {
    var f = new CommonsRequestLoggingFilter();
    f.setIncludeClientInfo(true);
    f.setIncludeQueryString(true);
    f.setIncludeHeaders(false);
    f.setIncludePayload(true);
    f.setMaxPayloadLength(10_000);
    return f;
  }
}