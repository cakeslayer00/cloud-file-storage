package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.ResourceDoesNotExistsException;
import com.vladsv.cloud_file_storage.mapper.MinioResourceMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import utils.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public void downloadResource(String path, Long userId, HttpServletResponse response) {
        path = PathUtils.getValidResourcePath(path, userId);

        if (!PathUtils.isDir(path)) {
            try (InputStream stream = minioRepository.getObject(BUCKET, path)) {
                StreamUtils.copy(stream, response.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            zipDirectoryContent(path, response);
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + path + "\"");
    }

    private List<String> getDirectoryContent(String path) {
        Iterable<Result<Item>> results = minioRepository.listObjects(BUCKET, path);
        List<String> resources = new ArrayList<>();
        results.forEach(result -> {
            try {
                resources.add(result.get().objectName());
            } catch (ErrorResponseException | InsufficientDataException | InternalException |
                     InvalidKeyException | InvalidResponseException | IOException |
                     NoSuchAlgorithmException | ServerException | XmlParserException e) {
                throw new RuntimeException(e);
            }
        });
        return resources;

    }

    private void zipDirectoryContent(String path, HttpServletResponse response) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            List<String> resources = getDirectoryContent(path);

            resources.forEach(resource -> {
                try (InputStream in = minioRepository.getObject(BUCKET, resource)) {
                    String substring = resource.substring(path.length());
                    ZipEntry zipEntry = new ZipEntry(substring);
                    zipOutputStream.putNextEntry(zipEntry);
                    in.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            zipOutputStream.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
