package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.ConflictingResourceException;
import com.vladsv.cloud_file_storage.exception.IncompatibleResourceTransmissionException;
import com.vladsv.cloud_file_storage.exception.InvalidResourcePathException;
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
import org.springframework.web.multipart.MultipartFile;
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

    private static final String DIRECTORY_TO_FILE_TRANSMIT_FORBIDDEN = "Moving directory resource to file resource is forbidden";
    private static final String SOURCE_TO_EXISTING_TARGET_TRANSIT = "Transit of source path resource to target path results with conflict";
    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource with given name does not exist";
    private static final String INVALID_OR_MISSING_PATH = "There is no resource under path %s or it's missing";
    private static final String CONFLICT_RESOURCE_UPLOAD = "Resource with matching name already uploaded";

    private static final String BUCKET = "user-files";
    private static final String DUMMY_FILE = ".init";
    private static final String ROOT = "";

    private final MinioRepository minioRepository;

    public ResourceResponseDto getResourceStat(String path, Long id) {
        path = PathUtils.getValidRootResourcePath(path, id);

        if (PathUtils.isDir(path)) {
            path += DUMMY_FILE;
        }

        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(BUCKET, path), id);
    }

    public void downloadResource(String path, Long id, HttpServletResponse response) {
        path = PathUtils.getValidRootResourcePath(path, id);

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

    public void deleteResource(String path, Long id) {
        path = PathUtils.getValidRootResourcePath(path, id);

        if (!minioRepository.isResourceExists(BUCKET, path)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS);
        }

        if (PathUtils.isDir(path)) {
            minioRepository.removeObjects(BUCKET, getDirectoryContentRecursive(path));
        } else {
            minioRepository.removeObject(BUCKET, path);
        }
    }

    public void moveOrRenameResource(String source, String target, Long id) {
        source = PathUtils.getValidRootResourcePath(source, id);
        target = PathUtils.getValidRootResourcePath(target, id);

        if (!minioRepository.isResourceExists(BUCKET, source)) {
            throw new InvalidResourcePathException(INVALID_OR_MISSING_PATH.formatted(source));
        }

        if (minioRepository.isResourceExists(BUCKET, target)) {
            throw new ConflictingResourceException(SOURCE_TO_EXISTING_TARGET_TRANSIT);
        }

        if (PathUtils.isDir(source)) {
            handleDirectoryMove(source, target);
        } else {
            handleFileMove(source, target);
        }
    }

    public List<ResourceResponseDto> searchFromPrefix(String query, Long id) {
        query = PathUtils.getValidRootResourcePath(query, id);

        Iterable<Result<Item>> results = minioRepository.listObjectsRecursive(BUCKET, query);
        List<ResourceResponseDto> resources = new ArrayList<>();
        results.forEach(result -> resources.add(MinioResourceMapper.INSTANCE.toResourceDto(result, id)));
        return resources;
    }

    public ResourceResponseDto uploadResource(String path, MultipartFile file, Long id) {
        path = PathUtils.getValidRootResourcePath(path, id) + file.getOriginalFilename();

        if (minioRepository.isResourceExists(BUCKET, path)) {
            throw new ConflictingResourceException(CONFLICT_RESOURCE_UPLOAD);
        }

        try {
            minioRepository.putObject(BUCKET, path, file.getInputStream(), file.getSize());
            return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(BUCKET, path), id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<ResourceResponseDto> searchFromRoot(String query, Long id) {
        String userRootPrefix = PathUtils.getUserRootDirectoryPrefix(id);

        Iterable<Result<Item>> results = minioRepository.listObjectsRecursive(BUCKET, ROOT);
        List<ResourceResponseDto> resources = new ArrayList<>();
        results.forEach(result -> {
            try {
                String substring = result.get().objectName().substring(userRootPrefix.length());
                if (substring.contains(query)) {
                    resources.add(MinioResourceMapper.INSTANCE.toResourceDto(result, id));
                }
            } catch (ErrorResponseException | InsufficientDataException | InternalException |
                     InvalidKeyException | InvalidResponseException | IOException |
                     NoSuchAlgorithmException | ServerException | XmlParserException e) {
                throw new RuntimeException(e);
            }
        });
        return resources;
    }

    private void handleFileMove(String source, String target) {
        moveFile(source, PathUtils.isDir(target)
                ? target + PathUtils.getFileName(source)
                : target);
    }

    private void handleDirectoryMove(String source, String target) {
        if (!PathUtils.isDir(target)) {
            throw new IncompatibleResourceTransmissionException(DIRECTORY_TO_FILE_TRANSMIT_FORBIDDEN);
        }
        moveDirectory(source, target);
    }

    private void moveFile(String source, String target) {
        minioRepository.copyObject(BUCKET, source, target);
        minioRepository.removeObject(BUCKET, source);
    }

    private void moveDirectory(String source, String target) {
        List<String> sourceDirContent = getDirectoryContentRecursive(source);
        sourceDirContent.forEach(path -> handleFileMove(path, target));
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

    private List<String> getDirectoryContentRecursive(String path) {
        Iterable<Result<Item>> results = minioRepository.listObjectsRecursive(BUCKET, path);
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
}
