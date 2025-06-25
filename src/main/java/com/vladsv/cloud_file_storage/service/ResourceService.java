package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.InvalidResourcePathException;
import com.vladsv.cloud_file_storage.exception.InvalidResourceUploadBodyException;
import com.vladsv.cloud_file_storage.exception.ResourceAlreadyExistsException;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static utils.PathUtils.isDir;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource under path '%s' doesn't exist";
    private static final String RESOURCE_ALREADY_EXISTS = "Resource '%s' already in destination folder";
    private static final String INVALID_SOURCE_OR_TARGET_PATH = "Either source or target path is invalid or missing";
    private static final String ROOT_DIRECTORY_REMOVAL_ATTEMPT = "You cannot delete root folder;)";
    private static final String NO_FILES_PROVIDED_FOR_UPLOAD = "No files provided for upload";

    private final MinioRepository minioRepository;

    public ResourceResponseDto getResource(String path, Long id) {
        String rootDirectory = PathUtils.getUserRootDirectoryPattern(id);
        String relativePath = PathUtils.normalizePath(path);
        String absolutePath = rootDirectory + relativePath;

        if (!minioRepository.isResourceExists(absolutePath)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(relativePath));
        }

        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(absolutePath), id);
    }

    public void deleteResource(String path, Long id) {
        String rootDirectory = PathUtils.getUserRootDirectoryPattern(id);
        String relativePath = PathUtils.normalizePath(path);
        String absolutePath = rootDirectory + relativePath;

        if (absolutePath.equals(rootDirectory)) {
            throw new InvalidResourcePathException(ROOT_DIRECTORY_REMOVAL_ATTEMPT);
        }

        if (!minioRepository.isResourceExists(absolutePath)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(relativePath));
        }

        if (isDir(path)) {
            minioRepository.removeObjects(getDirectoryContent(absolutePath, true));
        } else {
            minioRepository.removeObject(absolutePath);
        }
    }

    public void downloadResource(String path, Long id, HttpServletResponse response) {
        String rootDirectory = PathUtils.getUserRootDirectoryPattern(id);
        String relativePath = PathUtils.normalizePath(path);
        String absolutePath = rootDirectory + relativePath;

        if (!minioRepository.isResourceExists(absolutePath)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(relativePath));
        }

        if (!isDir(relativePath)) {
            downloadSingleFile(absolutePath, response);
        } else {
            downloadDirectoryAsZip(absolutePath, response);
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + relativePath + "\"");
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

        String rootDirectory = PathUtils.getUserRootDirectoryPattern(userId);
        String absoluteSource = PathUtils.normalizePath(rootDirectory + source);
        String absoluteTarget = PathUtils.normalizePath(rootDirectory + target);

        if (!minioRepository.isResourceExists(absoluteSource)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(source));
        }

        if (minioRepository.isResourceExists(absoluteTarget)) {
            throw new ResourceAlreadyExistsException(RESOURCE_ALREADY_EXISTS.formatted(target));
        }

        if (isSimpleRename(absoluteSource, absoluteTarget)) {
            return renameResource(absoluteSource, absoluteTarget, userId);
        }
        return moveResource(absoluteSource, absoluteTarget, userId);
    }

    public List<ResourceResponseDto> search(String query, Long userId) {
        String rootDirectory = PathUtils.getUserRootDirectoryPattern(userId);

        Iterable<Result<Item>> iterable = minioRepository.listObjects(rootDirectory, true);
        Stream<Item> ItemsStream = StreamSupport.stream(iterable.spliterator(), false).map(this::unwrapResult);

        return ItemsStream
                .filter(item -> PathUtils.getResourceNameFromPath(item.objectName()).contains(query))
                .map(item -> MinioResourceMapper.INSTANCE.toResourceDto(item, userId))
                .toList();
    }

    public List<ResourceResponseDto> uploadResources(String path, List<MultipartFile> files, Long id) {
        if (files == null || files.isEmpty()) {
            throw new InvalidResourceUploadBodyException(NO_FILES_PROVIDED_FOR_UPLOAD);
        }

        String rootDirectory = PathUtils.getUserRootDirectoryPattern(id);
        String relativePath = PathUtils.normalizePath(path);
        String absolutePath = rootDirectory + relativePath;

        return files.stream()
                .map(file -> uploadSingleFile(absolutePath, file, id))
                .toList();

    }

    private void downloadSingleFile(String path, HttpServletResponse response) {
        try (InputStream stream = minioRepository.getObject(path)) {
            StreamUtils.copy(stream, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadDirectoryAsZip(String path, HttpServletResponse response) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            List<String> resources = getDirectoryContent(path, true);

            for (String resource : resources) {
                if (resource.equals(path)) {
                    continue;
                }
                addFileToZip(path, resource, zipOutputStream);
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
        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(target), userId);
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

    private ResourceResponseDto uploadSingleFile(String path, MultipartFile file, Long id) {
        String absolute = path + file.getOriginalFilename();
        if (minioRepository.isResourceExists(absolute)) {
            throw new InvalidResourcePathException(
                    RESOURCE_ALREADY_EXISTS.formatted(file.getOriginalFilename()));
        }
        createParentDirectoriesIfNeeded(path, file.getOriginalFilename());

        minioRepository.putObject(absolute, file);
        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(absolute), id);
    }

    private void createParentDirectoriesIfNeeded(String path, String relativeFilePath) {
        String pathToFile = PathUtils.getPathToResource(relativeFilePath);

        if (!pathToFile.contains("/") || pathToFile.startsWith("/")) {
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

    private List<String> getDirectoryContent(String path) {
        return getDirectoryContent(path, false);
    }

    private List<String> getDirectoryContent(String absolutePath, boolean recursive) {
        Iterable<Result<Item>> iterable = minioRepository.listObjects(absolutePath, recursive);
        Stream<Item> ItemsStream = StreamSupport.stream(iterable.spliterator(), false).map(this::unwrapResult);
        return ItemsStream.map(Item::objectName).collect(Collectors.toList());
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

}
