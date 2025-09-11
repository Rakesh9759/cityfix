package com.cityfix.api.repo;

import com.cityfix.api.domain.issue.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

  @Query(
    value = """
      SELECT * FROM issue
      WHERE (:status IS NULL OR status = :status)
        AND (:category IS NULL OR category = :category)
        AND (:ward IS NULL OR ward_code = :ward)
        AND (
              :hasBbox = false
              OR ST_Intersects(
                  geom,
                  ST_MakeEnvelope(:minLon,:minLat,:maxLon,:maxLat,4326)::geography
                 )
            )
      ORDER BY created_at DESC
      """,
    countQuery = """
      SELECT count(*) FROM issue
      WHERE (:status IS NULL OR status = :status)
        AND (:category IS NULL OR category = :category)
        AND (:ward IS NULL OR ward_code = :ward)
        AND (
              :hasBbox = false
              OR ST_Intersects(
                  geom,
                  ST_MakeEnvelope(:minLon,:minLat,:maxLon,:maxLat,4326)::geography
                 )
            )
      """,
    nativeQuery = true
  )
  Page<Issue> search(
      @Param("status") String status,
      @Param("category") String category,
      @Param("ward") String ward,
      @Param("hasBbox") boolean hasBbox,
      @Param("minLon") Double minLon,
      @Param("minLat") Double minLat,
      @Param("maxLon") Double maxLon,
      @Param("maxLat") Double maxLat,
      Pageable pageable
  );
}