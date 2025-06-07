package com.vladsv.cloud_file_storage.repository;

import com.vladsv.cloud_file_storage.exception.DirectoryAlreadyExistsException;
import com.vladsv.cloud_file_storage.exception.DirectoryDoesNotExistsException;
import com.vladsv.cloud_file_storage.exception.ResourceAlreadyExistsException;
import com.vladsv.cloud_file_storage.exception.ResourceDoesNotExistsException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MinioRepository {

    private static final String DIRECTORY_DOES_NOT_EXISTS = "Directory under path '%s' does not exist or not valid";
    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource under path '%s' does not exist or not valid";
    private static final String DIRECTORY_ALREADY_EXISTS = "Directory under path '%s' already exists";
    private static final String RESOURCE_ALREADY_EXISTS = "Resource under path '%s' already exists";

    private static final String COMMON_BUCKET = "user-files";

    private final MinioClient minioClient;

    public GenericResponse commenceDirectory(String path) {
        try {
            path = path.endsWith("/") ? path : path + "/";

            if (isDirectoryExists(path)) {
                throw new DirectoryAlreadyExistsException(
                        String.format(DIRECTORY_ALREADY_EXISTS, path));
            }

            return minioClient.putObject(
                    PutObjectArgs.builder().bucket(COMMON_BUCKET).object(path + ".init").stream(
                                    new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());

        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getAllByPrefix(String path) {
        try {
            List<String> res = new ArrayList<>();
            Iterable<Result<Item>> resources = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(COMMON_BUCKET).prefix(path).recursive(true).build());

            for (Result<Item> result: resources) {
                Item item = result.get();
                res.add(item.objectName());
            }

            return res;
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getDirectoriesByPrefix(String path) {
        path = path.endsWith("/") ? path : path + "/";

        if (!isDirectoryExists(path)) {
            throw new DirectoryDoesNotExistsException(
                    String.format(DIRECTORY_DOES_NOT_EXISTS, path));
        }

        return getAllByPrefix(path);
    }

    public InputStream getResource(String path) {
        try {
            if (!isDirectoryExists(path)) {
                throw new DirectoryDoesNotExistsException(String.format(DIRECTORY_DOES_NOT_EXISTS, path));
            }

            if (!isObjectExists(path)) {
                throw new ResourceDoesNotExistsException(String.format(RESOURCE_DOES_NOT_EXISTS, path));
            }

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(COMMON_BUCKET)
                            .object(path)
                            .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public StatObjectResponse getResourceStat(String path) {
        try {
            if (!isDirectoryExists(path)) {
                throw new DirectoryDoesNotExistsException(String.format(DIRECTORY_DOES_NOT_EXISTS, path));
            }

            if (!isObjectExists(path)) {
                throw new ResourceDoesNotExistsException(String.format(RESOURCE_DOES_NOT_EXISTS, path));
            }

            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(COMMON_BUCKET)
                    .object(path).build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyResource(String source, String target) {
        try {
            if (!isDirectoryExists(source)) {
                throw new DirectoryDoesNotExistsException(String.format(DIRECTORY_DOES_NOT_EXISTS, source));
            }

            if (!isObjectExists(source)) {
                throw new ResourceDoesNotExistsException(String.format(RESOURCE_DOES_NOT_EXISTS, source));
            }

            if (!isDirectoryExists(target)) {
                throw new DirectoryDoesNotExistsException(String.format(DIRECTORY_DOES_NOT_EXISTS, target));
            }

            if (isObjectExists(target)) {
                throw new ResourceAlreadyExistsException(String.format(RESOURCE_ALREADY_EXISTS, target));
            }

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(COMMON_BUCKET)
                            .object(target)
                            .source(
                                    CopySource.builder()
                                            .bucket(COMMON_BUCKET)
                                            .object(source)
                                            .build())
                            .build());
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String path) {
        try {
            if (!isDirectoryExists(path)) {
                throw new DirectoryDoesNotExistsException(String.format(DIRECTORY_DOES_NOT_EXISTS, path));
            }

            if (!isObjectExists(path)) {
                throw new ResourceDoesNotExistsException(String.format(RESOURCE_DOES_NOT_EXISTS, path));
            }

            minioClient.removeObject(RemoveObjectArgs.builder().bucket(COMMON_BUCKET).object(path).build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDirectory(String path) {
        return path.endsWith("/");
    }

    private boolean isDirectoryExists(String path) {
        return isObjectExists(getPathWithResourceNameExcluded(path) + ".init");
    }

    private boolean isObjectExists(String name) {
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

    private String getPathWithResourceNameExcluded(String payloadPath) {
        int index = payloadPath.lastIndexOf("/");
        return payloadPath.substring(0, index + 1);
    }

}
