package com.cityfix.api.service;

import com.cityfix.api.config.RabbitConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ImageIngestPublisher {
  private final RabbitTemplate rabbit;

  public ImageIngestPublisher(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
  }

  public void publish(UUID issueId, String key) {
    Map<String, Object> payload = Map.of(
        "schema", "v1",
        "type", "image.uploaded",
        "issueId", issueId.toString(),
        "objectKey", key,
        "at", System.currentTimeMillis()
    );

    CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());

    rabbit.convertAndSend(
        RabbitConfig.EXCHANGE,
        RabbitConfig.RK_IMAGE_INGEST,
        payload,
        message -> {
          message.getMessageProperties().setContentType("application/json");
          message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          message.getMessageProperties().setHeader("x-cityfix-schema", "v1");
          message.getMessageProperties().setHeader("x-cityfix-type", "image.uploaded");
          message.getMessageProperties().setHeader("x-issue-id", issueId.toString());
          return message;
        },
        correlation
    );
  }
}
