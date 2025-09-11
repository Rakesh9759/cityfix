package com.cityfix.api.service;

import com.cityfix.api.domain.issue.*;
import com.cityfix.api.repo.IssueRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IssueService {
  private final IssueRepository repo;

  public IssueService(IssueRepository repo) { this.repo = repo; }

  public Issue create(Issue i) { return repo.save(i); }

  public Page<Issue> search(
      IssueStatus status, Category category, String ward,
      Double minLon, Double minLat, Double maxLon, Double maxLat,
      int page, int size
  ) {
    boolean hasBbox = minLon!=null && minLat!=null && maxLon!=null && maxLat!=null;
    Pageable pageable = PageRequest.of(page, size);
    return repo.search(
        status==null ? null : status.name(),
        category==null ? null : category.name(),
        (ward==null || ward.isBlank()) ? null : ward,
        hasBbox, minLon, minLat, maxLon, maxLat,
        pageable
    );
  }

  public Optional<Issue> get(UUID id) { return repo.findById(id); }
}