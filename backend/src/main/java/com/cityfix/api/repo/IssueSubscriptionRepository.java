package com.cityfix.api.repo;

import com.cityfix.api.domain.issue.IssueSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueSubscriptionRepository extends JpaRepository<IssueSubscription, UUID> {
  List<IssueSubscription> findAllByIssue_Id(UUID issueId);
  Optional<IssueSubscription> findByIssue_IdAndEmailIgnoreCase(UUID issueId, String email);
  Optional<IssueSubscription> findByUnsubscribeToken(String token);
}