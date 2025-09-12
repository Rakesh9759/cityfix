package com.cityfix.api.repo;

import com.cityfix.api.domain.issue.IssueEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IssueEventRepository extends JpaRepository<IssueEvent, UUID> {}