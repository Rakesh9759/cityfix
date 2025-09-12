package com.cityfix.api.repo;

import com.cityfix.api.domain.issue.IssueImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueImageRepository extends JpaRepository<IssueImage, UUID> {
  List<IssueImage> findAllByIssue_IdOrderByCreatedAtAsc(UUID issueId);
}