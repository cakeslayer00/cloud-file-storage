package com.vladsv.cloud_file_storage.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.minio")
public record MinioProperties(@NotBlank @URL String endpoint,
                              @NotBlank String accessKey,
                              @NotBlank String secretKey,
                              @NotBlank String bucketName) {

}
