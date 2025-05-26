package com.vladsv.cloud_file_storage.repository;

import com.vladsv.cloud_file_storage.exception.FolderAlreadyExistsException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class MinioRepository {

    private static final String FOLDER_WITH_THIS_PATH_ALREADY_EXISTS = "Folder under path %s already exists";
    private static final String COMMON_BUCKET = "user-files";

    private final MinioClient minioClient;

    public void createEmptyFolder(String path) {
        try {
            if (isObjectExist(path)) {
                throw new FolderAlreadyExistsException(
                        String.format(FOLDER_WITH_THIS_PATH_ALREADY_EXISTS, path));
            }

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(COMMON_BUCKET).object(path).stream(
                                    new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public StatObjectResponse getObject(String path) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(COMMON_BUCKET)
                    .object(path).build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isObjectExist(String name) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(COMMON_BUCKET)
                    .object(name).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
