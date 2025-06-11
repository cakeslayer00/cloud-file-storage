package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.ResourceDoesNotExistsException;
import com.vladsv.cloud_file_storage.mapper.MinioResourceMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utils.PathUtils;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource with given name does not exist";

    private static final String BUCKET = "user-files";
    private static final String DUMMY_FILE = ".init";

    private final MinioRepository minioRepository;

    public ResourceResponseDto getResourceStat(String path, Long userId) {
        path = PathUtils.getValidResourcePath(path, userId);

        if (!minioRepository.isResourceExists(BUCKET, path)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS);
        }

        if (PathUtils.isDir(path)) {
            path += DUMMY_FILE;
        }

        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(BUCKET, path), userId);
    }

}
