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

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String DIRECTORY_TO_FILE_TRANSMIT_FORBIDDEN = "Moving directory resource to file resource is forbidden";
    private static final String SOURCE_TO_EXISTING_TARGET_TRANSIT = "Transit of source path resource to target path results with conflict";
    private static final String RESOURCE_DOES_NOT_EXISTS = "Resource with name '%s' doesn't exist";
    private static final String INVALID_OR_MISSING_PATH = "There is no resource under path %s or it's missing";
    private static final String CONFLICT_RESOURCE_UPLOAD = "Resource with matching name already uploaded";

    private static final String DUMMY_FILE = ".init";
    public static final String ROOT_DIRECTORY_REMOVAL_ATTEMPT = "You cannot delete root folder";

    private final MinioRepository minioRepository;

    public ResourceResponseDto getResource(String path, Long id) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String normal = PathUtils.normalizePath(path);

        if (!minioRepository.isResourceExists(root + normal)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(normal));
        }

        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(root + normal), id);
    }

    public void deleteResource(String path, Long id) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String normal = PathUtils.normalizePath(path);

        if (normal.isEmpty()) {
            throw new InvalidResourcePathException(ROOT_DIRECTORY_REMOVAL_ATTEMPT);
        }

        if (!minioRepository.isResourceExists(root + normal)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(normal));
        }

        if (PathUtils.isDir(path)) {
            minioRepository.removeObjects(getDirectoryContent(root + normal, true));
        } else {
            minioRepository.removeObject(root + normal);
        }
    }

    public void downloadResource(String path, Long id, HttpServletResponse response) {
        String root = PathUtils.getUserRootDirectoryPattern(id);
        String normal = PathUtils.normalizePath(path);

        if (!minioRepository.isResourceExists(root + normal)) {
            throw new ResourceDoesNotExistsException(RESOURCE_DOES_NOT_EXISTS.formatted(normal));
        }

        if (!PathUtils.isDir(normal)) {
            downloadSingleFile(root + normal, response);
        } else {
            downloadDirectoryAsZip(root, normal, response);
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + normal + "\"");
    }

    private void downloadSingleFile(String path, HttpServletResponse response) {
        try (InputStream stream = minioRepository.getObject(path)) {
            StreamUtils.copy(stream, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadDirectoryAsZip(String root, String normal, HttpServletResponse response) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            List<String> resources = getDirectoryContent(root + normal, true);

            for (String resource : resources) {
                if (resource.endsWith(normal)) {
                    continue;
                }
                addFileToZip(root + normal, resource, zipOutputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addFileToZip(String path,
                              String resource,
                              ZipOutputStream zipOutputStream) throws IOException {
        try (InputStream in = minioRepository.getObject(resource)) {
            String substring = resource.substring(path.length());
            ZipEntry zipEntry = new ZipEntry(substring);
            zipOutputStream.putNextEntry(zipEntry);
            in.transferTo(zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    public ResourceResponseDto moveOrRenameResource(String source, String target, Long id) {
        source = PathUtils.getValidRootResourcePath(source, id);
        target = PathUtils.getValidRootResourcePath(target, id);

        if (source.equals(target)) {
            throw new ConflictingResourceException(SOURCE_TO_EXISTING_TARGET_TRANSIT);
        }

        if (!minioRepository.isResourceExists(source)) {
            throw new InvalidResourcePathException(INVALID_OR_MISSING_PATH.formatted(source));
        }

        return PathUtils.isDir(source)
                ? handleDirectoryMove(source, target, id)
                : handleFileMove(source, target, id);
    }

    public List<ResourceResponseDto> searchFromPrefix(String query, Long id) {
        query = PathUtils.getValidRootResourcePath(query, id);

        Iterable<Result<Item>> results = minioRepository.listObjects(query);
        List<ResourceResponseDto> resources = new ArrayList<>();
        results.forEach(result -> resources.add(MinioResourceMapper.INSTANCE.toResourceDto(result, id)));
        return resources;
    }

    public ResourceResponseDto uploadResource(String path, MultipartFile file, Long id) {
        path = PathUtils.getValidRootResourcePath(path, id) + file.getOriginalFilename();

        if (minioRepository.isResourceExists(path)) {
            throw new ConflictingResourceException(CONFLICT_RESOURCE_UPLOAD);
        }

        try {
            minioRepository.putObject(path, file.getInputStream(), file.getSize());
            return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(path), id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private ResourceResponseDto getResourceStatWithValidPath(String path, Long id) {
        if (PathUtils.isDir(path)) {
            path += DUMMY_FILE;
        }

        return MinioResourceMapper.INSTANCE.toResourceDto(minioRepository.statObject(path), id);
    }

    private ResourceResponseDto handleFileMove(String source, String target, Long id) {
        if (PathUtils.isDir(target)) {
            return moveFile(source, target + PathUtils.getFileName(source), id);
        }
        return moveFile(source, target, id);
    }

    private ResourceResponseDto handleDirectoryMove(String source, String target, Long id) {
        if (!PathUtils.isDir(target)) {
            throw new IncompatibleResourceTransmissionException(DIRECTORY_TO_FILE_TRANSMIT_FORBIDDEN);
        }
        return moveDirectory(source, target, id);
    }

    private ResourceResponseDto moveFile(String source, String target, Long id) {
        minioRepository.copyObject(source, target);
        minioRepository.removeObject(source);
        return getResourceStatWithValidPath(target, id);
    }

    private ResourceResponseDto moveDirectory(String source, String target, Long id) {
        List<String> sourceDirContent = getDirectoryContent(source, true);
        sourceDirContent.forEach(path -> handleFileMove(path, target, id));
        return getResourceStatWithValidPath(target, id);
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
