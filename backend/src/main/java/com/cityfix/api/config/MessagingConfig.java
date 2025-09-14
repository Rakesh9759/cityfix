package com.cityfix.api.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
  @Bean
  public TopicExchange eventsExchange() {
    // durable, not auto-delete
    return new TopicExchange("cityfix.events", true, false);
  }
}
