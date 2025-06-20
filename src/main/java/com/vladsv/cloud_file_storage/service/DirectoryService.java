package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.DirectoryAlreadyExistsException;
import com.vladsv.cloud_file_storage.exception.DirectoryDoesNotExistsException;
import com.vladsv.cloud_file_storage.mapper.MinioResourceMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import io.minio.messages.Item;
import jakarta.transaction.Transactional;
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
    private static final String DIRECTORY_DOES_NOT_EXISTS = "Directory with name '%s' does not exist";
    private static final String DIRECTORY_ALREADY_EXISTS = "Directory with name '%s' already exists";

    private final MinioRepository minioRepository;

    public void createRootDirectory(String path) {
        minioRepository.putEmptyObject(path);
    }

    @Transactional
    public ResourceResponseDto createEmptyDirectory(String path, Long id) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String normal = PathUtils.normalizePath(path);
        String relative = normal.isEmpty() ? normal : PathUtils.applyDirectorySuffix(normal);

        if (minioRepository.isResourceExists(root + relative)) {
            throw new DirectoryAlreadyExistsException(DIRECTORY_ALREADY_EXISTS.formatted(normal));
        }

        minioRepository.putEmptyObject(root + relative);
        StatObjectResponse statObjectResponse = minioRepository.statObject(root + relative);
        return MinioResourceMapper.INSTANCE.toResourceDto(statObjectResponse, id);
    }

    public List<ResourceResponseDto> getDirectoryContent(String path, Long id) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String normal = PathUtils.normalizePath(path);
        String relative = normal.isEmpty() ? normal : PathUtils.applyDirectorySuffix(normal);
        Iterable<Result<Item>> resources = minioRepository.listObjects(root + relative);

        if (!resources.iterator().hasNext()) {
            throw new DirectoryDoesNotExistsException(DIRECTORY_DOES_NOT_EXISTS.formatted(normal));
        }

        List<ResourceResponseDto> res = new ArrayList<>();
        resources.forEach(resource -> {
            if (isRootDir(resource, relative, root)) return;
            res.add(MinioResourceMapper.INSTANCE.toResourceDto(resource, id));
        });
        return res;
    }

    private boolean isRootDir(Result<Item> resource, String relative, String root) {
        try {
            String path = resource.get().objectName().substring(root.length());
            if (relative.isEmpty() && path.isEmpty()) {
                return true;
            }

            if (!relative.isEmpty() && path.endsWith(relative)) {
                return true;
            }
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

}
