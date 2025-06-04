package com.vladsv.cloud_file_storage.repository;

import com.vladsv.cloud_file_storage.exception.DirectoryDoesNotExistException;
import com.vladsv.cloud_file_storage.exception.FolderAlreadyExistsException;
import com.vladsv.cloud_file_storage.exception.ResourceDoesNotExistException;
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

    private static final String DIRECTORY_DOES_NOT_EXIST = "Directory under path '%s' doesn't exist or not valid";
    private static final String RESOURCE_DOES_NOT_EXIST = "Resource under path '%s' does not exist";
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

    public List<String> getAllResourcesFromDirectory(String path) {
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

    public InputStream getResource(String path) {
        try {
            if (!isDirectoryExists(path)) {
                throw new DirectoryDoesNotExistException(String.format(DIRECTORY_DOES_NOT_EXIST, path));
            }

            if (!isObjectExist(path)) {
                path = path + ".init";
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
                throw new DirectoryDoesNotExistException(String.format(DIRECTORY_DOES_NOT_EXIST, path));
            }

            if (!isObjectExist(path)) {
                throw new ResourceDoesNotExistException(String.format(RESOURCE_DOES_NOT_EXIST, path));
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

    public void delete(String path) {
        try {
            if (!isDirectoryExists(path)) {
                throw new DirectoryDoesNotExistException(String.format(DIRECTORY_DOES_NOT_EXIST, path));
            }

            if (!isObjectExist(path)) {
                throw new ResourceDoesNotExistException(String.format(RESOURCE_DOES_NOT_EXIST, path));
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
        return isObjectExist(getPathResourceExcluded(path) + ".init");
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

    private String getPathResourceExcluded(String payloadPath) {
        int index = payloadPath.lastIndexOf("/");
        return payloadPath.substring(0, index + 1);
    }

}
