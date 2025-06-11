package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.DirectoryAlreadyExistsException;
import com.vladsv.cloud_file_storage.exception.DirectoryDoesNotExistsException;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import utils.PathUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DirectoryService {

    private static final String DIRECTORY_DOES_NOT_EXISTS = "Directory with given name does not exist";
    private static final String DIRECTORY_ALREADY_EXISTS = "Directory with given name already exists";

    private static final String BUCKET = "user-files";
    private static final String DUMMY_FILE = ".init";

    private final MinioRepository minioRepository;

    public List<ResourceResponseDto> getDirResources(String origin, Long userId) {
        String rootPath = PathUtils.getValidRootDirectoryPath(origin, userId);
        String prefix = PathUtils.getUserRootDirectoryPrefix(userId);
        Iterable<Result<Item>> resources = minioRepository.listObjects(BUCKET, rootPath);

        if (!resources.iterator().hasNext()) {
            throw new DirectoryDoesNotExistsException(DIRECTORY_DOES_NOT_EXISTS);
        }

        List<ResourceResponseDto> res = new ArrayList<>();
        resources.forEach(resource -> res.add(mapToResourceResponseDto(prefix, resource)));
        return res;
    }

    public void createEmptyDirectory(String path, Long userId) {
        String rootPath = PathUtils.getValidRootDirectoryPath(path, userId);

        if (minioRepository.isResourceExists(BUCKET, rootPath)) {
            throw new DirectoryAlreadyExistsException(DIRECTORY_ALREADY_EXISTS);
        }

        minioRepository.putEmptyObject(BUCKET, rootPath + DUMMY_FILE);
    }

    private ResourceResponseDto mapToResourceResponseDto(String userRootDirPrefix, Result<Item> resource) {
        try {
            Item item = resource.get();

            String relative = item.objectName().substring(userRootDirPrefix.length());

            String trimmed = item.isDir()
                    ? relative.substring(0, relative.length() - 1)
                    : relative;

            int lastSlash = trimmed.lastIndexOf("/");
            String name = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
            String path = lastSlash > 0 ? trimmed.substring(0, lastSlash + 1) : "/";

            return new ResourceResponseDto(path, name, item.size(), item.isDir() ? "DIRECTORY" : "FILE");
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }
}
