package com.cityfix.api.service;

import com.cityfix.api.config.RabbitConfig;
import com.cityfix.api.domain.issue.Issue;
import com.cityfix.api.domain.issue.IssueEvent;
import com.cityfix.api.domain.issue.IssueImage;
import com.cityfix.api.repo.IssueEventRepository;
import com.cityfix.api.repo.IssueImageRepository;
import com.cityfix.api.service.ExifService.ExifResult;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
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

  public ImageIngestListener(FileStorageService storage, ThumbnailService thumbs, ExifService exif,
                             IssueService issues, IssueImageRepository images, IssueEventRepository events) {
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

      // resolve the file
      Path path = storage.resolve(key);
      if (!Files.exists(path)) {
        writeEvent(issueId, "image_read_failed", "{\"reason\":\"file_not_found\",\"key\":\"" + esc(key) + "\"}");
        return;
      }

      writeEvent(issueId, "ingest_started", "{\"key\":\"" + esc(key) + "\"}");

      // EXIF
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
            issues.create(issue); // save
            writeEvent(issueId, "exif_location_detected",
                "{\"lat\":" + xr.lat() + ",\"lon\":" + xr.lon() +
                (xr.takenAt()!=null? ",\"takenAt\":\"" + xr.takenAt() + "\"" : "") + "}");
          }
        }
      } catch (Exception ex) {
        writeEvent(issueId, "image_read_failed", "{\"reason\":\"exif_parse_error\",\"message\":\"" + esc(ex.getMessage()) + "\"}");
      }

      // Thumbnail (always attempt)
      var thumb = thumbs.make512(path, storage.resolve("").getParent(), issueId);
      IssueImage thumbRow = new IssueImage();
      thumbRow.setIssue(issues.get(issueId).orElseThrow());
      thumbRow.setStorageKey(thumb.key());
      thumbRow.setContentType("image/jpeg");
      thumbRow.setWidth(thumb.width());
      thumbRow.setHeight(thumb.height());
      thumbRow.setThumb(true);
      images.save(thumbRow);

      writeEvent(issueId, "thumbnail_generated",
          "{\"thumbKey\":\"" + esc(thumb.key()) + "\",\"width\":" + thumb.width() + ",\"height\":" + thumb.height() + "}");
      writeEvent(issueId, "ingest_completed", "{}");

    } catch (Exception e) {
      // swallow to ack and move on, but record something if we can
      // (If you want dead-lettering, add a DLX/DLQ and rethrow here.)
    }
  }

  private void writeEvent(UUID issueId, String type, String payloadJson) {
    IssueEvent ev = new IssueEvent();
    ev.setIssue(issues.get(issueId).orElse(null));
    if (ev.getIssue() == null) return; // issue deleted, nothing to do
    ev.setType(type);
    ev.setPayload(payloadJson);
    events.save(ev);
  }

  private static String esc(String s) { return s == null ? "" : s.replace("\"","\\\""); }
}