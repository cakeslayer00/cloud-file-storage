package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static utils.PathUtils.isDir;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource with name '%s' doesn't exist";
    private static final String INVALID_SOURCE_OR_TARGET_PATH = "Either source or target path is invalid or missing";
    private static final String CONFLICT_RESOURCE_UPLOAD = "Resource with name '%s' already uploaded";
    private static final String ROOT_DIRECTORY_REMOVAL_ATTEMPT = "You cannot delete root folder;)";
    public static final String FILE_ALREADY_EXISTS_DURING_UPLOAD = "File '%s' already in destination folder";
    public static final String NO_FILES_PROVIDED_FOR_UPLOAD = "No files provided for upload";

    private final MinioRepository minioRepository;

    public ResourceResponseDto getResource(String path, Long id) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String relative = PathUtils.normalizePath(path);

        if (!minioRepository.isResourceExists(root + relative)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(relative));
        }

        return getResourceResponseDto(id, root + relative);
    }

    public void deleteResource(String path, Long id) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String relative = PathUtils.normalizePath(path);

        if (relative.isEmpty()) {
            throw new InvalidResourcePathException(ROOT_DIRECTORY_REMOVAL_ATTEMPT);
        }

        if (!minioRepository.isResourceExists(root + relative)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(relative));
        }

        if (isDir(path)) {
            minioRepository.removeObjects(getDirectoryContent(root + relative, true));
        } else {
            minioRepository.removeObject(root + relative);
        }
    }

    public void downloadResource(String path, Long id, HttpServletResponse response) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String relative = PathUtils.normalizePath(path);

        if (!minioRepository.isResourceExists(root + relative)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(relative));
        }

        if (!isDir(relative)) {
            downloadSingleFile(root + relative, response);
        } else {
            downloadDirectoryAsZip(root, relative, response);
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + relative + "\"");
    }

    private void downloadSingleFile(String path, HttpServletResponse response) {
        try (InputStream stream = minioRepository.getObject(path)) {
            StreamUtils.copy(stream, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadDirectoryAsZip(String root, String relative, HttpServletResponse response) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            List<String> resources = getDirectoryContent(root + relative, true);

            for (String resource : resources) {
                if (resource.endsWith(relative)) {
                    continue;
                }
                addFileToZip(root + relative, resource, zipOutputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addFileToZip(String path, String resource, ZipOutputStream zipOutputStream) throws IOException {
        try (InputStream in = minioRepository.getObject(resource)) {
            String substring = resource.substring(path.length());
            ZipEntry zipEntry = new ZipEntry(substring);
            zipOutputStream.putNextEntry(zipEntry);
            in.transferTo(zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    public ResourceResponseDto moveOrRenameResource(String source, String target, long userId) {
        source = PathUtils.normalizePath(source);
        target = PathUtils.normalizePath(target);

        if (source.equals(target)) {
            throw new InvalidResourcePathException(INVALID_SOURCE_OR_TARGET_PATH);
        }

        boolean sourceIsDir = isDir(source);
        boolean targetIsDir = isDir(target);
        if (sourceIsDir != targetIsDir) {
            throw new InvalidResourcePathException(INVALID_SOURCE_OR_TARGET_PATH);
        }

        String root = PathUtils.getUserRootDirectoryPattern(userId);
        String normalizedSource = PathUtils.normalizePath(root + source);
        String normalizedTarget = PathUtils.normalizePath(root + target);

        if (!minioRepository.isResourceExists(normalizedSource)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(source));
        }

        if (isSimpleRename(normalizedSource, normalizedTarget)) {
            return renameResource(normalizedSource, normalizedTarget, userId);
        }
        return moveResource(normalizedSource, normalizedTarget, userId);
    }

    private boolean isSimpleRename(String source, String target) {
        return PathUtils.getPathToResource(source).equals(PathUtils.getPathToResource(target));
    }

    private ResourceResponseDto renameResource(String source, String target, long userId) {
        if (!isSimpleRename(source, target)) {
            throw new InvalidResourcePathException(INVALID_SOURCE_OR_TARGET_PATH);
        }
        return performMoveOperation(source, target, userId);
    }

    private ResourceResponseDto moveResource(String source, String target, long userId) {
        return performMoveOperation(source, target, userId);
    }

    private ResourceResponseDto performMoveOperation(String source, String target, long userId) {
        if (isDir(source)) {
            moveDirectoryContents(source, target);
        } else {
            moveSingleFile(source, target);
        }
        return getResourceResponseDto(userId, target);
    }

    private void moveDirectoryContents(String source, String target) {
        List<String> resources = getDirectoryContent(source);

        resources.forEach(resource -> {
            String relativePath = resource.substring(source.length());
            minioRepository.copyObject(resource, target + relativePath);
        });

        minioRepository.removeObjects(resources);
    }

    private void moveSingleFile(String source, String target) {
        minioRepository.copyObject(source, target);
        minioRepository.removeObject(source);
    }

    public List<ResourceResponseDto> uploadResources(String path, MultipartFile[] files, Long id) {
        if (files == null || files.length == 0) {
            throw new InvalidResourceUploadBodyException(NO_FILES_PROVIDED_FOR_UPLOAD);
        }

        String rootPrefix = PathUtils.getUserRootDirectoryPattern(id);
        String normalized = PathUtils.normalizePath(path);
        String relativePath = rootPrefix + normalized;

        return Arrays.stream(files)
                .map(file -> uploadSingleFile(relativePath, file, id))
                .toList();

    }

    private ResourceResponseDto uploadSingleFile(String path, MultipartFile file, Long id) {
        String absolute = path + file.getOriginalFilename();
        if (minioRepository.isResourceExists(absolute)) {
            throw new InvalidResourcePathException(
                    FILE_ALREADY_EXISTS_DURING_UPLOAD.formatted(file.getOriginalFilename()));
        }
        createParentDirectoriesIfNeeded(path, file.getOriginalFilename());

        minioRepository.putObject(absolute, file);
        return getResourceResponseDto(id, absolute);
    }

    private void createParentDirectoriesIfNeeded(String path, String relativeFilePath) {
        String pathToFile = PathUtils.getPathToResource(relativeFilePath);

        if (!pathToFile.contains("/")) {
            return;
        }

        Arrays.stream(pathToFile.split("/"))
                .reduce(path, (accumulatedPath, directory) -> {
                    String currentPath = accumulatedPath + directory + "/";
                    createDirectoryIfNotExists(currentPath);
                    return currentPath;
                });
    }

    private void createDirectoryIfNotExists(String path) {
        if (!minioRepository.isResourceExists(path)) {
            minioRepository.putEmptyObject(path);
        }
    }

    public List<ResourceResponseDto> searchFromPrefix(String query, Long id) {
        query = PathUtils.getValidRootResourcePath(query, id);

        Iterable<Result<Item>> results = minioRepository.listObjects(query);
        List<ResourceResponseDto> resources = new ArrayList<>();
        results.forEach(result -> resources.add(MinioResourceMapper.INSTANCE.toResourceDto(result, id)));
        return resources;
    }

    public List<ResourceResponseDto> searchFromRoot(String query, Long id) {
        String userRootPrefix = PathUtils.getUserRootDirectoryPattern(id);

        Iterable<Result<Item>> results = minioRepository.listObjects(userRootPrefix);
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

    private List<String> getDirectoryContent(String path) {
        return getDirectoryContent(path, false);
    }

    private List<String> getDirectoryContent(String path, boolean recursive) {
        Iterable<Result<Item>> results = minioRepository.listObjects(path, recursive);
        List<String> resources = new ArrayList<>();
        results.forEach(result -> {
            try {
                String resultingPath = result.get().objectName();
                resources.add(resultingPath);
            } catch (ErrorResponseException | InsufficientDataException | InternalException |
                     InvalidKeyException | InvalidResponseException | IOException |
                     NoSuchAlgorithmException | ServerException | XmlParserException e) {
                throw new RuntimeException(e);
            }
        });
        return resources;
    }

    private ResourceResponseDto getResourceResponseDto(Long id, String absolute) {
        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(absolute), id);
    }

}
