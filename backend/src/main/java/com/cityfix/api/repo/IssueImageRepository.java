// package com.cityfix.api.repo;

// import com.cityfix.api.domain.issue.IssueImage;
// import org.springframework.data.jpa.repository.JpaRepository;

// import java.util.List;
// import java.util.UUID;

// public interface IssueImageRepository extends JpaRepository<IssueImage, UUID> {
//   List<IssueImage> findAllByIssue_IdOrderByCreatedAtAsc(UUID issueId);
// }

package com.cityfix.api.repo;

import com.cityfix.api.domain.issue.IssueImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueImageRepository extends JpaRepository<IssueImage, UUID> {
  // existing (oldest → newest)
  List<IssueImage> findAllByIssue_IdOrderByCreatedAtAsc(UUID issueId);

  // used by IssueController.listImages() (newest → oldest)
  List<IssueImage> findAllByIssueIdOrderByCreatedAtDesc(UUID issueId);
  // If you prefer the underscore style, this equivalent also works:
  // List<IssueImage> findAllByIssue_IdOrderByCreatedAtDesc(UUID issueId);
}
