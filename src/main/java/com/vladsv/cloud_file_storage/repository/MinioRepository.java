package com.vladsv.cloud_file_storage.repository;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.*;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import utils.PathUtils;

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
    private static final String INVALID_PATH_REQUEST_OR_MISSING = "Invalid path request or it's missing";

    private static final String COMMON_BUCKET = "user-files";

    private final MinioClient minioClient;

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

    //TODO: Pretty big method, seek ways to break down.
    public List<ResourceResponseDto> getDirectoryResources(String origin, Long userId) {
        try {
            String rootPath = PathUtils.applyUserRootDirectoryPrefix(PathUtils.getValidPath(origin), userId);
            String userRootDirPrefix = PathUtils.getUserRootDirectoryPrefix(userId);

            if (!PathUtils.isDirectory(rootPath)) {
                throw new AmbiguousPathException(INVALID_PATH_REQUEST_OR_MISSING);
            }

            List<ResourceResponseDto> res = new ArrayList<>();
            Iterable<Result<Item>> resources = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(COMMON_BUCKET).prefix(rootPath).build());

            if (!resources.iterator().hasNext()) {
                throw new DirectoryDoesNotExistsException(String.format(DIRECTORY_DOES_NOT_EXISTS, origin));
            }

            for (Result<Item> resource: resources) {
                Item item = resource.get();
                String objectName = item.objectName().substring(userRootDirPrefix.length());

                if (!item.isDir()) {
                    String relativePath = objectName.contains("/")
                            ? objectName.substring(0, objectName.lastIndexOf("/") + 1)
                            : "/";
                    String name = objectName.contains("/")
                            ? objectName.substring(objectName.lastIndexOf("/") + 1)
                            : objectName;

                    res.add(new ResourceResponseDto(relativePath, name, item.size(), "FILE"));
                } else {
                    String[] split = objectName.split("/");
                    String name = split[split.length - 1];
                    String join = String.join("/", split);
                    String relativePath = join.substring(0, join.lastIndexOf(name));

                    res.add(new ResourceResponseDto(relativePath.isEmpty()
                            ? "/"
                            : relativePath,
                            name, item.size(), "DIRECTORY"));
                }
            }

            return res;
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: change
    public void createDirectory(String path, Long userId) {
        try {
            String rootPath = PathUtils.applyDirectorySuffix(
                    PathUtils.applyUserRootDirectoryPrefix(
                        PathUtils.getValidPath(path), userId
                    ));

            if (isDirectoryExists(path)) {
                throw new DirectoryAlreadyExistsException(
                        String.format(DIRECTORY_ALREADY_EXISTS, path));
            }

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(COMMON_BUCKET).object(rootPath + ".init").stream(
                                    new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());

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
