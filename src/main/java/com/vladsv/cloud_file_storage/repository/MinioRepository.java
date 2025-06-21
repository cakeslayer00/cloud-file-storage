package com.vladsv.cloud_file_storage.repository;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioRepository {

    private final MinioClient minioClient;

    @Value("${spring.minio.bucket_name}")
    private String bucketName;

    public StatObjectResponse statObject(String path) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getObject(String path) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public void putObject(String path, MultipartFile file) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException | InternalException |
                 InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Result<Item>> listObjects(String path) {
        return listObjects(path, false);
    }

    public Iterable<Result<Item>> listObjects(String path, boolean recursive) {
        return minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(path)
                .recursive(recursive)
                .build());
    }

    public void putEmptyObject(String path) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException | InternalException |
                 InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyObject(String source, String target) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(target)
                    .source(CopySource.builder()
                            .bucket(bucketName)
                            .object(source)
                            .build())
                    .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException | InternalException |
                 InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeObject(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeObjects(List<String> objects) {
        try {
            List<DeleteObject> deleteObjectList = new LinkedList<>(objects.stream()
                    .map(DeleteObject::new)
                    .toList());
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(deleteObjectList)
                    .build());
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.debug("Error in deleting object {}; {}", error.objectName(), error.message());
            }
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isResourceExists(String name) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(name)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
