package com.vladsv.cloud_file_storage.config;

import com.vladsv.cloud_file_storage.config.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) throws Exception {
        MinioClient client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey()).build();

        boolean isBucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucketName()).build());

        if (!isBucketExists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucketName()).build());
        }

        return client;
    }

}
