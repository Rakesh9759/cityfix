package com.cityfix.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

import java.net.URI;

@Configuration
public class SesConfig {

  @Bean
  public SesClient sesClient(
      @Value("${aws.region:us-east-1}") String region,
      @Value("${aws.ses.endpoint:http://localhost:4566}") String endpoint) {

    return SesClient.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test","test")
        ))
        .endpointOverride(URI.create(endpoint))
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }
}