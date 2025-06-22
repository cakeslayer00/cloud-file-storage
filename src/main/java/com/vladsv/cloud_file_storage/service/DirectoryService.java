package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.ResourceAlreadyExistsException;
import com.vladsv.cloud_file_storage.exception.ResourceDoesNotExistsException;
import com.vladsv.cloud_file_storage.mapper.MinioResourceMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import utils.PathUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class DirectoryService {
    private static final String DIRECTORY_DOES_NOT_EXISTS = "Directory with name '%s' does not exist";
    private static final String DIRECTORY_ALREADY_EXISTS = "Directory with name '%s' already exists";

    private final MinioRepository minioRepository;

    public void createRootDirectory(String root) {
        minioRepository.putEmptyObject(root);
    }

    @Transactional
    public ResourceResponseDto createEmptyDirectory(String path, Long userId) {
        String rootDirectory = PathUtils.getUserRootDirectoryPattern(userId);
        String normalized = PathUtils.normalizePath(path);
        String relativePath = normalized.isEmpty() ? normalized : PathUtils.applyDirectorySuffix(normalized);
        String absolutePath = rootDirectory + relativePath;

        if (minioRepository.isResourceExists(absolutePath)) {
            throw new ResourceAlreadyExistsException(DIRECTORY_ALREADY_EXISTS.formatted(normalized));
        }

        minioRepository.putEmptyObject(absolutePath);
        StatObjectResponse statObjectResponse = minioRepository.statObject(absolutePath);
        return MinioResourceMapper.INSTANCE.toResourceDto(statObjectResponse, userId);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponseDto> getDirectoryContent(String path, Long userId) {
        String rootDirectory = PathUtils.getUserRootDirectoryPattern(userId);
        String normalized = PathUtils.normalizePath(path);
        String relative = normalized.isEmpty() ? normalized : PathUtils.applyDirectorySuffix(normalized);
        String absolutePath = rootDirectory + relative;

        if (!minioRepository.isResourceExists(absolutePath)) {
            throw new ResourceDoesNotExistsException(DIRECTORY_DOES_NOT_EXISTS.formatted(normalized));
        }

        Iterable<Result<Item>> iterable = minioRepository.listObjects(absolutePath);
        Stream<Item> ItemsStream = StreamSupport.stream(iterable.spliterator(), false).map(this::unwrapResult);
        return ItemsStream
                .filter(item -> !isRootDirectory(item, rootDirectory))
                .map(item -> MinioResourceMapper.INSTANCE.toResourceDto(item, userId))
                .collect(Collectors.toList());
    }

    private Item unwrapResult(Result<Item> itemResult) {
        try {
            return itemResult.get();
        } catch (ErrorResponseException | InvalidKeyException | InvalidResponseException |
                 IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException | InternalException | InsufficientDataException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isRootDirectory(Item resource, String rootDirectory) {
        return resource.objectName().equals(rootDirectory);
    }

}
