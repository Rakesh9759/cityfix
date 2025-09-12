package com.cityfix.api.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class ExifService {

  public static record ExifResult(Double lat, Double lon, OffsetDateTime takenAt) {}

  public ExifResult extract(InputStream in) throws Exception {
    Metadata meta = ImageMetadataReader.readMetadata(in);

    Double lat = null, lon = null;
    OffsetDateTime takenAt = null;

    // ---- GPS
    GpsDirectory gps = meta.getFirstDirectoryOfType(GpsDirectory.class);
    if (gps != null && gps.getGeoLocation() != null) {
      double glat = gps.getGeoLocation().getLatitude();
      double glon = gps.getGeoLocation().getLongitude();
      if (!Double.isNaN(glat) && !Double.isNaN(glon)) {
        lat = glat;
        lon = glon;
      }
    }

    // ---- Taken-at (Date/Time Original)
    ExifSubIFDDirectory exif = meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
    if (exif != null) {
      Date d = exif.getDateOriginal();
      if (d != null) takenAt = OffsetDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
    }

    return new ExifResult(lat, lon, takenAt);
  }

  public static boolean plausible(Double lat, Double lon) {
    return lat != null && lon != null && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
  }
}
