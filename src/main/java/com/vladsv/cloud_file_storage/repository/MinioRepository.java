package com.vladsv.cloud_file_storage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import utils.PathUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class MinioRepository {

    private static final String DUMMY_FILE = ".init";

    private final MinioClient minioClient;

    public StatObjectResponse statObject(String bucket, String path) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(path).build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public GetObjectResponse getObject(String bucket, String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(path).build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Result<Item>> listObjects(String bucket, String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).prefix(path).build());
    }

    public void putEmptyObject(String bucket, String path) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyObject(String bucket, String source, String target) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucket)
                            .object(target)
                            .source(
                                    CopySource.builder()
                                            .bucket(bucket)
                                            .object(source)
                                            .build())
                            .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeObject(String bucket, String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(path).build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isResourceExists(String bucket, String name) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(PathUtils.isDir(name) ? name + DUMMY_FILE : name).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
