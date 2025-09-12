package com.cityfix.api.web.dto;

import java.util.UUID;

public class ImageDtos {
  public record PresignResponse(String uploadUrl, long maxBytes, String[] allowedContentTypes) {}
  public record ImageResponse(UUID id, String contentType, Integer width, Integer height, String key) {}
}