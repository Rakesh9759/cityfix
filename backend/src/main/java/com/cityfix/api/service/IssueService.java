package com.cityfix.api.service;

import com.cityfix.api.domain.issue.Category;
import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.domain.issue.IssueEvent;
import com.cityfix.api.domain.issue.IssueStatus;
import com.cityfix.api.repo.IssueEventRepository;
import com.cityfix.api.repo.IssueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IssueService {

  private final IssueRepository repo;
  private final IssueEventRepository events;
  private final StatusEventPublisher statusPublisher;

  public IssueService(IssueRepository repo,
                      IssueEventRepository events,
                      StatusEventPublisher statusPublisher) {
    this.repo = repo;
    this.events = events;
    this.statusPublisher = statusPublisher;
  }

  // ----- Query/search passthrough (keeps existing controller working)
  public Page<Issue> search(IssueStatus status,
                            Category category,
                            String ward,
                            Double minLon, Double minLat,
                            Double maxLon, Double maxLat,
                            int page, int size) {
    return repo.search(status, category, ward, minLon, minLat, maxLon, maxLat, PageRequest.of(page, size));
  }

  public Optional<Issue> get(UUID id) { return repo.findById(id); }

  /** Create or update an Issue. */
  public Issue create(Issue i) { return repo.save(i); }

  // ----- Allowed transitions
  private static final Map<IssueStatus, EnumSet<IssueStatus>> ALLOWED = Map.of(
      IssueStatus.NEW,         EnumSet.of(IssueStatus.TRIAGED, IssueStatus.CLOSED),
      IssueStatus.TRIAGED,     EnumSet.of(IssueStatus.ASSIGNED, IssueStatus.IN_PROGRESS, IssueStatus.CLOSED),
      IssueStatus.ASSIGNED,    EnumSet.of(IssueStatus.IN_PROGRESS, IssueStatus.TRIAGED, IssueStatus.CLOSED),
      IssueStatus.IN_PROGRESS, EnumSet.of(IssueStatus.RESOLVED, IssueStatus.TRIAGED, IssueStatus.CLOSED),
      IssueStatus.RESOLVED,    EnumSet.of(IssueStatus.CLOSED, IssueStatus.IN_PROGRESS),
      IssueStatus.CLOSED,      EnumSet.noneOf(IssueStatus.class)
  );

  public List<IssueStatus> allowedNext(IssueStatus from) {
    return new ArrayList<>(ALLOWED.getOrDefault(from, EnumSet.noneOf(IssueStatus.class)));
  }

  // ----- Perform a transition + event + AMQP publish
  public Issue transition(UUID issueId, IssueStatus to, UUID actorUserId, String note) {
    Issue i = repo.findById(issueId).orElseThrow();
    IssueStatus from = i.getStatus();

    if (from == to) {
      throw new IllegalArgumentException("E9_SAME_STATE: Issue already in state " + to);
    }
    var allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(IssueStatus.class));
    if (!allowed.contains(to)) {
      throw new IllegalStateException("E9_INVALID_TRANSITION: " + from + " → " + to + " not allowed");
    }

    i.setStatus(to);
    repo.save(i);

    // timeline event
    IssueEvent ev = new IssueEvent();
    ev.setIssue(i);
    ev.setType("status_changed");
    String payload = "{\"from\":\"" + from + "\",\"to\":\"" + to + "\"" +
        (note != null && !note.isBlank() ? ",\"note\":\"" + note.replace("\"","\\\"") + "\"" : "") +
        "}";
    ev.setPayload(payload);
    ev.setActorUserId(actorUserId);
    events.save(ev);

    // AMQP publish (fire-and-forget)
    try {
      statusPublisher.publish(issueId, from.name(), to.name(), note);
    } catch (Exception ignored) {}

    return i;
  }
}
