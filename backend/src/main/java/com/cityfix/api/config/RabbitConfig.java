package com.cityfix.api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue; // AMQP Queue
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory; // correct package
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitConfig {
  public static final String EXCHANGE = "cityfix.issues";
  public static final String QUEUE_IMAGE_INGEST = "image.ingest";
  public static final String RK_IMAGE_INGEST = "image.ingest";
  public static final String QUEUE_STATUS_EVENTS = "status.events";
  public static final String RK_STATUS_CHANGED   = "issue.status.changed";
  public static final String EXCHANGE_EVENTS = "cityfix.events";
  public static final String RK_ISSUE_STATUS_CHANGED = "issue.status.changed"; // optional helper
  public static final String EXCHANGE_EVENTS = "cityfix.events";
  public static final String QUEUE_NOTIFICATIONS = "cityfix.notifications";
  public static final String RK_STATUS_CHANGED = "issue.status.changed";


  @Bean
  org.springframework.amqp.core.Queue statusEventsQueue() {
    return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_STATUS_EVENTS).build();
  }

  @Bean
  org.springframework.amqp.core.Binding statusChangedBinding(
      org.springframework.amqp.core.Queue statusEventsQueue, TopicExchange issuesExchange) {
    return org.springframework.amqp.core.BindingBuilder
        .bind(statusEventsQueue)
        .to(issuesExchange)
        .with(RK_STATUS_CHANGED);
  }


  @Bean
  TopicExchange issuesExchange() { return new TopicExchange(EXCHANGE, true, false); }

  @Bean
  Queue imageIngestQueue() { return QueueBuilder.durable(QUEUE_IMAGE_INGEST).build(); }

  @Bean
  Binding imageIngestBinding(Queue imageIngestQueue, TopicExchange issuesExchange) {
    return BindingBuilder.bind(imageIngestQueue).to(issuesExchange).with(RK_IMAGE_INGEST);
  }

  @Bean
  Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
    RabbitTemplate t = new RabbitTemplate(cf);
    t.setMessageConverter(conv);
    // you can keep the confirm/return callbacks you added in 8.G if present
    return t;
  }

  @Bean
  public TopicExchange cityfixEventsExchange() {
   return new TopicExchange(EXCHANGE_EVENTS, true, false);
  }

  @Bean
  SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
    SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(conv);
    f.setConcurrentConsumers(1);
    f.setMaxConcurrentConsumers(2);
    return f;
  }

  @Bean TopicExchange eventsExchange() { return new TopicExchange(EXCHANGE_EVENTS, true, false); }

  @Bean Queue notificationsQueue() { return QueueBuilder.durable(QUEUE_NOTIFICATIONS).build(); }

  @Bean Binding notificationsBinding() { return BindingBuilder.bind(notificationsQueue())
      .to(eventsExchange())
      .with(RK_STATUS_CHANGED);
}
}
