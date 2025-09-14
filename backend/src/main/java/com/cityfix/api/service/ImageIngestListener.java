package com.cityfix.api.service;

import com.cityfix.api.config.RabbitConfig;
import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.domain.issue.IssueEvent;
import com.cityfix.api.domain.issue.IssueImage;
import com.cityfix.api.repo.IssueEventRepository;
import com.cityfix.api.repo.IssueImageRepository;
import com.cityfix.api.service.ExifService.ExifResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageIngestListener {

  private final FileStorageService storage;
  private final ThumbnailService thumbs;
  private final ExifService exif;
  private final IssueService issues;
  private final IssueImageRepository images;
  private final IssueEventRepository events;

  public ImageIngestListener(FileStorageService storage,
                             ThumbnailService thumbs,
                             ExifService exif,
                             IssueService issues,
                             IssueImageRepository images,
                             IssueEventRepository events) {
    this.storage = storage;
    this.thumbs = thumbs;
    this.exif = exif;
    this.issues = issues;
    this.images = images;
    this.events = events;
  }

  @Transactional
  @RabbitListener(queues = RabbitConfig.QUEUE_IMAGE_INGEST)
  public void handle(Map<String, Object> payload) {
    try {
      UUID issueId = UUID.fromString(String.valueOf(payload.get("issueId")));
      String key = String.valueOf(payload.get("objectKey"));

      Path path = storage.resolve(key);
      if (!Files.exists(path)) {
        writeEvent(issueId, "image_read_failed", Map.of(
            "reason", "file_not_found",
            "key", key
        ));
        return;
      }

      writeEvent(issueId, "ingest_started", Map.of("key", key));

      // ---- EXIF
      try (InputStream in = Files.newInputStream(path)) {
        ExifResult xr = exif.extract(in);

        Optional<Issue> opt = issues.get(issueId);
        if (opt.isPresent()) {
          Issue issue = opt.get();
          boolean updated = false;

          if ((issue.getLat() == null || issue.getLon() == null) && ExifService.plausible(xr.lat(), xr.lon())) {
            issue.setLat(xr.lat());
            issue.setLon(xr.lon());
            issue.setLocationSource("exif");
            issue.setExifLat(xr.lat());
            issue.setExifLon(xr.lon());
            updated = true;
          }
          if (xr.takenAt() != null && issue.getExifTakenAt() == null) {
            issue.setExifTakenAt(xr.takenAt());
            updated = true;
          }
          if (updated) {
            issues.create(issue); // save changes

            Map<String, Object> exifPayload = new LinkedHashMap<>();
            exifPayload.put("lat", xr.lat());
            exifPayload.put("lon", xr.lon());
            if (xr.takenAt() != null) exifPayload.put("takenAt", xr.takenAt().toString());

            writeEvent(issueId, "exif_location_detected", exifPayload);
          }
        }
      } catch (Exception ex) {
        writeEvent(issueId, "image_read_failed", Map.of(
            "reason", "exif_parse_error",
            "message", ex.getMessage()
        ));
      }

      // ---- Thumbnail
      var thumb = thumbs.make512(path, storage.getRoot(), issueId);
      IssueImage thumbRow = new IssueImage();
      thumbRow.setIssue(issues.get(issueId).orElseThrow());
      // Entity likely maps storageKey -> s3_key column, or similar; keep your existing setter
      thumbRow.setStorageKey(thumb.key());
      thumbRow.setContentType("image/jpeg");
      thumbRow.setWidth(thumb.width());
      thumbRow.setHeight(thumb.height());
      thumbRow.setThumb(true);
      images.save(thumbRow);

      writeEvent(issueId, "thumbnail_generated", Map.of(
          "thumbKey", thumb.key(),
          "width", thumb.width(),
          "height", thumb.height()
      ));
      writeEvent(issueId, "ingest_completed", Map.of());

    } catch (Exception e) {
      // swallow to ack and move on (add DLQ later if desired)
    }
  }

  private void writeEvent(UUID issueId, String type, Map<String, Object> payload) {
    Issue i = issues.get(issueId).orElse(null);
    if (i == null) return;
    IssueEvent ev = new IssueEvent();
    ev.setIssue(i);
    ev.setType(type);
    ev.setPayload(payload); // Map<String,Object> -> jsonb via hibernate-types
    events.save(ev);
  }
}
