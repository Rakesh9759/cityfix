package com.cityfix.api.repo;

import com.cityfix.api.domain.issue.IssueEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueEventRepository extends JpaRepository<IssueEvent, UUID> {
  List<IssueEvent> findAllByIssue_IdOrderByCreatedAtAsc(UUID issueId);
}
