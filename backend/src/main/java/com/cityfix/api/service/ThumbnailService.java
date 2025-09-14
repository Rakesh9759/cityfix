package com.cityfix.api.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ThumbnailService {

  public static record ThumbSaved(String key, int width, int height) {}

  public ThumbSaved make512(Path original, Path uploadRoot, UUID issueId) throws IOException {
    Path dir = uploadRoot.resolve("thumb").resolve(issueId.toString());
    Files.createDirectories(dir);
    String name = UUID.randomUUID().toString() + "_512.jpg";
    Path out = dir.resolve(name);

    Thumbnails.of(original.toFile())
        .size(512, 512)
        .outputFormat("jpg")
        .outputQuality(0.85)
        .toFile(out.toFile());

    var img = javax.imageio.ImageIO.read(out.toFile());
    int w = img != null ? img.getWidth() : 512;
    int h = img != null ? img.getHeight() : 512;

    String key = "thumb/" + issueId + "/" + name;
    return new ThumbSaved(key, w, h);
  }
}
