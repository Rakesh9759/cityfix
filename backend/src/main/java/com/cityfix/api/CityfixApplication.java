package com.cityfix.api;

import com.cityfix.api.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import jakarta.annotation.PostConstruct;
import java.nio.file.*;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class CityfixApplication {

    private final StorageProperties storageProperties;

    public CityfixApplication(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public static void main(String[] args) {
        SpringApplication.run(CityfixApplication.class, args);
    }

    @PostConstruct
    void ensureUploadDirs() throws Exception {
        Path base = Path.of(storageProperties.getUploadDir());
        Path raw = base.resolve("raw");
        Path thumb = base.resolve("thumb");
        Files.createDirectories(raw);
        Files.createDirectories(thumb);
        System.out.println("Upload dirs ready at: " + base.toAbsolutePath());
    }
}
