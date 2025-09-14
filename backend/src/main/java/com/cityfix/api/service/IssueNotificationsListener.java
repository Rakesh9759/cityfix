package com.cityfix.api.service;

import com.cityfix.api.config.RabbitConfig;
import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.domain.issue.IssueEvent;
import com.cityfix.api.repo.IssueEventRepository;
import com.cityfix.api.repo.IssueRepository;
import com.cityfix.api.repo.IssueSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class IssueNotificationsListener {

  private final IssueRepository issues;
  private final IssueSubscriptionRepository subs;
  private final IssueEventRepository events;
  private final SesEmailService mail;
  private final ObjectMapper om;

  public IssueNotificationsListener(IssueRepository issues,
                                    IssueSubscriptionRepository subs,
                                    IssueEventRepository events,
                                    SesEmailService mail,
                                    ObjectMapper om) {
    this.issues = issues;
    this.subs = subs;
    this.events = events;
    this.mail = mail;
    this.om = om;
  }

  @Transactional
  @RabbitListener(queues = RabbitConfig.QUEUE_NOTIFICATIONS)
  public void handle(Map<String,Object> msg) {
    try {
      UUID issueId = UUID.fromString(String.valueOf(msg.get("issueId")));
      String oldStatus = String.valueOf(msg.get("oldStatus"));
      String newStatus = String.valueOf(msg.get("newStatus"));
      @SuppressWarnings("unchecked")
      Map<String,Object> metadata = (Map<String,Object>) msg.getOrDefault("metadata", Map.of());

      Issue issue = issues.findById(issueId).orElse(null);
      if (issue == null) return;

      var subscriptions = subs.findAllByIssue_Id(issueId);
      for (var s : subscriptions) {
        // build email model
        String unsubUrl = String.format("http://localhost:8080/v1/issues/%s/subscribe?token=%s",
            issueId, s.getUnsubscribeToken());

        Map<String,Object> model = new LinkedHashMap<>();
        model.put("issueId", issueId.toString());
        model.put("oldStatus", oldStatus);
        model.put("newStatus", newStatus);
        model.put("note", String.valueOf(metadata.getOrDefault("note","")));
        model.put("unsubscribeUrl", unsubUrl);

        String subject = "Issue " + issueId + " → " + newStatus;
        mail.sendIssueStatusEmail(s.getEmail(), subject, model);

        // write event
        IssueEvent ev = new IssueEvent();
        ev.setIssue(issue);
        ev.setType("email_notification_sent");
        ev.setPayload(Map.of(
            "email", s.getEmail(),
            "oldStatus", oldStatus,
            "newStatus", newStatus
        ));
        events.save(ev);
      }
    } catch (Exception ignore) {
      // swallow (dev); add DLQ/logging if you want
    }
  }
}