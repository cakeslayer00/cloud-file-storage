package com.vladsv.cloud_file_storage;

import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public interface Containers {

    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    MinIOContainer minio = new MinIOContainer("minio/minio:latest")
            .withUserName("username")
            .withPassword("password");
}
