// package com.cityfix.api.domain.issue;

// import com.cityfix.api.domain.AppUser;
// import jakarta.persistence.*;
// import java.time.OffsetDateTime;
// import jakarta.persistence.PrePersist;
// import java.util.UUID;

// @Entity
// @Table(name = "issue")
// public class Issue {
//   @Id
//   @Column(columnDefinition = "uuid")
//   private UUID id = UUID.randomUUID();

//   @Column(nullable = false)
//   private String title;

//   @Column(columnDefinition = "text")
//   private String description;

//   @Enumerated(EnumType.STRING)
//   @Column(nullable = false)
//   private Category category;

//   @Enumerated(EnumType.STRING)
//   @Column(nullable = false)
//   private Severity severity;

//   @Enumerated(EnumType.STRING)
//   @Column(nullable = false)

//   // reporter (nullable)
//   @ManyToOne(fetch = FetchType.LAZY)
//   @JoinColumn(name = "reported_by_user_id")
//   private AppUser reportedBy;

//   // geo (geom is generated in DB; we don't map it)
//   private Double lat;
//   private Double lon;

//   @Column(name = "approx_address")
//   private String approxAddress;

//   @Column(name = "ward_code")
//   private String wardCode;

//   @Column(name = "observed_at")
//   private OffsetDateTime observedAt;

//   @Column(name = "created_at", insertable = false, updatable = false)
//   private OffsetDateTime createdAt;

//   @Column(name = "updated_at", insertable = false, updatable = false)
//   private OffsetDateTime updatedAt;

//   @Column(name = "location_source")
//   private String locationSource; // user|exif|admin

//   @Column(name = "exif_lat")
//   private Double exifLat;

//   @Column(name = "exif_lon")
//   private Double exifLon;

//   @Column(name = "exif_taken_at")
//   private OffsetDateTime exifTakenAt;

//   // getters/setters
//   public UUID getId() { return id; }
//   public void setId(UUID id) { this.id = id; }

//   public String getTitle() { return title; }
//   public void setTitle(String title) { this.title = title; }

//   public String getDescription() { return description; }
//   public void setDescription(String description) { this.description = description; }

//   public Category getCategory() { return category; }
//   public void setCategory(Category category) { this.category = category; }

//   public Severity getSeverity() { return severity; }
//   public void setSeverity(Severity severity) { this.severity = severity; }

//   public IssueStatus getStatus() { return status; }
//   public void setStatus(IssueStatus status) { this.status = status; }

//   public AppUser getReportedBy() { return reportedBy; }
//   public void setReportedBy(AppUser reportedBy) { this.reportedBy = reportedBy; }

//   public Double getLat() { return lat; }
//   public void setLat(Double lat) { this.lat = lat; }

//   public Double getLon() { return lon; }
//   public void setLon(Double lon) { this.lon = lon; }

//   public String getApproxAddress() { return approxAddress; }
//   public void setApproxAddress(String approxAddress) { this.approxAddress = approxAddress; }

//   public String getWardCode() { return wardCode; }
//   public void setWardCode(String wardCode) { this.wardCode = wardCode; }

//   public OffsetDateTime getObservedAt() { return observedAt; }
//   public void setObservedAt(OffsetDateTime observedAt) { this.observedAt = observedAt; }

//   public OffsetDateTime getCreatedAt() { return createdAt; }
//   public OffsetDateTime getUpdatedAt() { return updatedAt; }

//   public String getLocationSource() { return locationSource; }
//   public void setLocationSource(String locationSource) { this.locationSource = locationSource; }

//   public Double getExifLat() { return exifLat; }
//   public void setExifLat(Double exifLat) { this.exifLat = exifLat; }

//   public Double getExifLon() { return exifLon; }
//   public void setExifLon(Double exifLon) { this.exifLon = exifLon; }

//   public OffsetDateTime getExifTakenAt() { return exifTakenAt; }
//   public void setExifTakenAt(OffsetDateTime exifTakenAt) { this.exifTakenAt = exifTakenAt; }
// }


package com.cityfix.api.domain.issue;

import com.cityfix.api.domain.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue")
public class Issue {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id = UUID.randomUUID();

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Severity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IssueStatus status; // <— was missing

  // reporter (nullable)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_by_user_id")
  private AppUser reportedBy;

  // geo (geom is generated in DB; we don't map it)
  private Double lat;
  private Double lon;

  @Column(name = "approx_address")
  private String approxAddress;

  @Column(name = "ward_code")
  private String wardCode;

  @Column(name = "observed_at")
  private OffsetDateTime observedAt;

  // DB-managed timestamps (defaults/triggers). Keep insertable/updatable false.
  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "location_source")
  private String locationSource; // user|exif|admin

  @Column(name = "exif_lat")
  private Double exifLat;

  @Column(name = "exif_lon")
  private Double exifLon;

  @Column(name = "exif_taken_at")
  private OffsetDateTime exifTakenAt;

  // --- Lifecycle hooks ---
  @PrePersist
  void prePersist() {
    if (status == null) status = IssueStatus.NEW;
  }

  // --- Getters / Setters ---
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public Category getCategory() { return category; }
  public void setCategory(Category category) { this.category = category; }

  public Severity getSeverity() { return severity; }
  public void setSeverity(Severity severity) { this.severity = severity; }

  public IssueStatus getStatus() { return status; }
  public void setStatus(IssueStatus status) { this.status = status; }

  public AppUser getReportedBy() { return reportedBy; }
  public void setReportedBy(AppUser reportedBy) { this.reportedBy = reportedBy; }

  public Double getLat() { return lat; }
  public void setLat(Double lat) { this.lat = lat; }

  public Double getLon() { return lon; }
  public void setLon(Double lon) { this.lon = lon; }

  public String getApproxAddress() { return approxAddress; }
  public void setApproxAddress(String approxAddress) { this.approxAddress = approxAddress; }

  public String getWardCode() { return wardCode; }
  public void setWardCode(String wardCode) { this.wardCode = wardCode; }

  public OffsetDateTime getObservedAt() { return observedAt; }
  public void setObservedAt(OffsetDateTime observedAt) { this.observedAt = observedAt; }

  public OffsetDateTime getCreatedAt() { return createdAt; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }

  public String getLocationSource() { return locationSource; }
  public void setLocationSource(String locationSource) { this.locationSource = locationSource; }

  public Double getExifLat() { return exifLat; }
  public void setExifLat(Double exifLat) { this.exifLat = exifLat; }

  public Double getExifLon() { return exifLon; }
  public void setExifLon(Double exifLon) { this.exifLon = exifLon; }

  public OffsetDateTime getExifTakenAt() { return exifTakenAt; }
  public void setExifTakenAt(OffsetDateTime exifTakenAt) { this.exifTakenAt = exifTakenAt; }
}
