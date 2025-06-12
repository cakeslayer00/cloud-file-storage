package com.vladsv.cloud_file_storage.repository;

import com.vladsv.cloud_file_storage.exception.ResourceDoesNotExistsException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import utils.PathUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MinioRepository {

    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource with given name does not exist";

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

    public InputStream getObject(String bucket, String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(path).build());
        } catch (ErrorResponseException e) {
            //TODO: not sure whether this right, check out/sdf
            if (e.getMessage().contains("The specified key does not exist")) {
                throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS);
            }
            throw new RuntimeException(e);
        } catch (InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Result<Item>> listObjects(String bucket, String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).prefix(path).build());
    }

    public Iterable<Result<Item>> listObjectsRecursive(String bucket, String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).prefix(path).recursive(true).build());
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

    public void removeObjects(String bucket, List<String> objects) {
        try {
            List<DeleteObject> deleteObjectList = new LinkedList<>(
                    objects.stream().map(DeleteObject::new).toList());
            Iterable<Result<DeleteError>> results =
                    minioClient.removeObjects(
                            RemoveObjectsArgs.builder().bucket(bucket).objects(deleteObjectList).build());
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                //TODO: add logging
                System.out.println(
                        "Error in deleting object " + error.objectName() + "; " + error.message());
            }
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
