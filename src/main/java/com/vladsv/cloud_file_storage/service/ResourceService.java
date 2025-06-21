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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static utils.PathUtils.isDir;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource with name '%s' doesn't exist";
    private static final String INVALID_OR_MISSING_PATH = "Either source or target path is invalid or missing";
    private static final String CONFLICT_RESOURCE_UPLOAD = "Resource with name '%s' already uploaded";
    private static final String CONFLICT_RESOURCE_MANIPULATION = "Source and target resources have same path;) ";
    private static final String FORBIDDEN_RESOURCE_MANIPULATION = "Moving or renaming directory to file is forbidden";
    private static final String ROOT_DIRECTORY_REMOVAL_ATTEMPT = "You cannot delete root folder;)";
    private static final String ROOT_DIRECTORY_MANIPULATION_ATTEMPT = "You cannot manipulate root folder;)";

    private final MinioRepository minioRepository;

    public ResourceResponseDto getResource(String path, Long id) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String relative = PathUtils.normalizePath(path);

        if (!minioRepository.isResourceExists(root + relative)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(relative));
        }

        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(root + relative), id);
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
            throw new InvalidResourcePathException(INVALID_OR_MISSING_PATH);
        }

        boolean sourceIsDir = isDir(source);
        boolean targetIsDir = isDir(target);
        if (sourceIsDir != targetIsDir) {
            throw new InvalidResourcePathException(INVALID_OR_MISSING_PATH);
        }

        String root = PathUtils.getUserRootDirectoryPattern(userId);
        String normalizedSource = PathUtils.normalizePath(root + source);
        String normalizedTarget = PathUtils.normalizePath(root + target);

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
            throw new InvalidResourcePathException(INVALID_OR_MISSING_PATH);
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

    public ResourceResponseDto uploadResource(String path, MultipartFile file, Long id) {
        path = PathUtils.getValidRootResourcePath(path, id) + file.getOriginalFilename();

        if (minioRepository.isResourceExists(path)) {
            throw new ConflictingResourceException(CONFLICT_RESOURCE_UPLOAD.formatted(file.getOriginalFilename()));
        }

        try {
            minioRepository.putObject(path, file.getInputStream(), file.getSize());
            return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(path), id);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

}
