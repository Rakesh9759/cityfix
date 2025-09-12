package com.cityfix.api.service;

import com.cityfix.api.config.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

  private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png");

  private final Path root;

  public record Saved(String key, String contentType, Integer width, Integer height) {}

  public FileStorageService(StorageProperties props) {
    this.root = Path.of(props.getUploadDir());
  }

  public Saved saveRaw(UUID issueId, String originalFilename, String contentType, InputStream in) throws IOException {
    if (contentType == null || !ALLOWED.contains(contentType)) {
      throw new IllegalArgumentException("Unsupported content type. Allowed: image/jpeg, image/png");
    }
    String ext = contentType.equals("image/png") ? "png" : "jpg";
    String safeBase = UUID.randomUUID().toString();
    String fileName = safeBase + "." + ext;

    Path dir = root.resolve("raw").resolve(issueId.toString());
    Files.createDirectories(dir);
    Path dest = dir.resolve(fileName);

    // Write to disk
    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);

    // Read dimensions (best-effort)
    Integer w = null, h = null;
    try (InputStream read = Files.newInputStream(dest)) {
      BufferedImage img = ImageIO.read(read);
      if (img != null) { w = img.getWidth(); h = img.getHeight(); }
    } catch (Exception ignored) {}

    // return relative key (under upload root)
    String key = "raw/" + issueId + "/" + fileName;
    return new Saved(key, contentType, w, h);
  }

  public Path resolve(String key) {
    // prevent path traversal
    if (!StringUtils.hasText(key) || key.contains("..")) throw new IllegalArgumentException("Bad key");
    return root.resolve(key).normalize();
  }

  public Path getRoot() {
    return root;
  }
}
