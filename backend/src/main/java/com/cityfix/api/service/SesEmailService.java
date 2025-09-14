package com.cityfix.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Map;

@Service
public class SesEmailService {
  private final SesClient ses;
  private final TemplateEngine thymeleaf;
  private final String from;

  public SesEmailService(SesClient ses, TemplateEngine thymeleaf,
                         @Value("${aws.ses.from:no-reply@cityfix.local}") String from) {
    this.ses = ses;
    this.thymeleaf = thymeleaf;
    this.from = from;
  }

  public void sendIssueStatusEmail(String to, String subject, Map<String,Object> model) {
    String html = render("email/issue-status", model);
    String text = renderText("email/issue-status.txt", model);

    Destination dest = Destination.builder().toAddresses(to).build();

    Message msg = Message.builder()
        .subject(Content.builder().data(subject).build())
        .body(Body.builder()
            .text(text != null ? Content.builder().data(text).build() : null)
            .html(Content.builder().data(html).build())
            .build())
        .build();

    ses.sendEmail(SendEmailRequest.builder()
        .destination(dest)
        .source(from)
        .message(msg)
        .build());
  }

  public void sendVerifyLink(String to, String verifyUrl) {
    Map<String,Object> m = Map.of("verifyUrl", verifyUrl);
    String html = render("email/verify-email", m);
    String text = renderText("email/verify-email.txt", m);

    ses.sendEmail(SendEmailRequest.builder()
        .source(from)
        .destination(Destination.builder().toAddresses(to).build())
        .message(Message.builder()
            .subject(Content.builder().data("Verify your CityFix email").build())
            .body(Body.builder()
                .text(text != null ? Content.builder().data(text).build() : null)
                .html(Content.builder().data(html).build())
                .build())
            .build())
        .build());
  }

  private String render(String template, Map<String,Object> model) {
    Context ctx = new Context();
    model.forEach(ctx::setVariable);
    return thymeleaf.process(template, ctx);
  }

  private String renderText(String template, Map<String,Object> model) {
    try { return render(template, model); }
    catch (Exception ignore) { return null; }
  }
}