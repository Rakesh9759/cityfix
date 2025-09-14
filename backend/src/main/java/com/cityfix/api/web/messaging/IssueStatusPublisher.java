package com.cityfix.api.web.messaging;

import com.cityfix.api.domain.issue.IssueStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class IssueStatusPublisher {

  private static final Logger log = LoggerFactory.getLogger(IssueStatusPublisher.class);

  private final RabbitTemplate rabbitTemplate;
  private final String exchange;
  private final String routingKey;

  public IssueStatusPublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${app.messaging.events-exchange:cityfix.events}") String exchange,
      @Value("${app.messaging.status-routing-key:issue.status.changed}") String routingKey
  ) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
    this.routingKey = routingKey;
  }

  public void publishStatusChanged(UUID issueId, IssueStatus oldStatus, IssueStatus newStatus, Map<String,Object> metadata) {
    Map<String,Object> payload = new HashMap<>();
    payload.put("issueId", issueId.toString());
    payload.put("oldStatus", oldStatus != null ? oldStatus.name() : null);
    payload.put("newStatus", newStatus != null ? newStatus.name() : null);
    if (metadata != null && !metadata.isEmpty()) payload.put("metadata", metadata);

    if (rabbitTemplate != null) {
      rabbitTemplate.convertAndSend(exchange, routingKey, payload);
      log.info("Published status change [{} -> {}] for issue {} to {}:{}",
          oldStatus, newStatus, issueId, exchange, routingKey);
    } else {
      log.info("AMQP not configured; would publish to {}:{} payload={}", exchange, routingKey, payload);
    }
  }
}
