package com.cityfix.api.service;

import com.cityfix.api.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ImageIngestPublisher {
  private final RabbitTemplate rabbit;

  public ImageIngestPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }

  public void publish(UUID issueId, String key) {
    var payload = Map.of(
        "issueId", issueId.toString(),
        "objectKey", key,
        "at", System.currentTimeMillis()
    );
    rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_IMAGE_INGEST, payload);
  }
}