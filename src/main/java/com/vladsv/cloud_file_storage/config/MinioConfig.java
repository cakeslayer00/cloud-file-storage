package com.vladsv.cloud_file_storage.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    //TODO: Not sure whether it's even working, fix later
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(System.getenv("MINIO_ENDPOINT"))
                .credentials(System.getenv("MINIO_ACCESS_KEY"), System.getenv("MINIO_SECRET_KEY"))
                .build();
    }

}
