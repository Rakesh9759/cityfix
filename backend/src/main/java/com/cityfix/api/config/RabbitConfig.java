package com.cityfix.api.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
  public static final String EXCHANGE = "cityfix.issues";
  public static final String QUEUE_IMAGE_INGEST = "image.ingest";
  public static final String RK_IMAGE_INGEST = "image.ingest";

  @Bean TopicExchange issuesExchange() { return new TopicExchange(EXCHANGE, true, false); }

  @Bean Queue imageIngestQueue() { return QueueBuilder.durable(QUEUE_IMAGE_INGEST).build(); }

  @Bean Binding imageIngestBinding(Queue imageIngestQueue, TopicExchange issuesExchange) {
    return BindingBuilder.bind(imageIngestQueue).to(issuesExchange).with(RK_IMAGE_INGEST);
  }
}