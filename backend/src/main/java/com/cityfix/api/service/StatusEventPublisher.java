package com.cityfix.api.service;

import com.cityfix.api.config.RabbitConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class StatusEventPublisher {

  private final RabbitTemplate rabbit;

  public StatusEventPublisher(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
  }

  public void publish(UUID issueId, String from, String to, String note) {
    var payload = Map.of(
        "schema", "v1",
        "type", "issue.status.changed",
        "issueId", issueId.toString(),
        "from", from,
        "to", to,
        "note", note == null ? "" : note,
        "at", System.currentTimeMillis()
    );

    var cd = new CorrelationData(UUID.randomUUID().toString());

    rabbit.convertAndSend(
        RabbitConfig.EXCHANGE,
        RabbitConfig.RK_STATUS_CHANGED,
        payload,
        msg -> {
          msg.getMessageProperties().setContentType("application/json");
          msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          msg.getMessageProperties().setHeader("x-cityfix-type", "issue.status.changed");
          return msg;
        },
        cd
    );
  }
}