package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import utils.PathUtils;

import static utils.PathUtils.isDir;

@Mapper
public interface MinioResourceMapper {

    MinioResourceMapper INSTANCE = Mappers.getMapper(MinioResourceMapper.class);

    default ResourceResponseDto toResourceDto(Item item, Long userId) {
        if (item == null) {
            return null;
        }
        String relativePath = item.objectName().substring(PathUtils.getUserRootDirectoryPattern(userId).length());

        String path = PathUtils.getPathToResource(relativePath);
        String name = PathUtils.getResourceNameFromPath(relativePath);
        Long size = item.isDir() ? null : item.size();
        String type = item.isDir() ? "DIRECTORY" : "FILE";

        return new ResourceResponseDto(path, name, size, type);
    }

    default ResourceResponseDto toResourceDto(StatObjectResponse response, Long id) {
        if (response == null) {
            return null;
        }

        String relativePath = response.object().substring(PathUtils.getUserRootDirectoryPattern(id).length());
        relativePath = relativePath.isEmpty() ? "/" : relativePath;

        String path = PathUtils.getPathToResource(relativePath);
        String name = PathUtils.getResourceNameFromPath(relativePath);
        Long size = isDir(relativePath) ? null : response.size();
        String type = isDir(relativePath) ? "DIRECTORY" : "FILE";

        return new ResourceResponseDto(path, name, size, type);
    }

}
