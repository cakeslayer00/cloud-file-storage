package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.minio.StatObjectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import utils.PathUtils;

@Mapper
public interface MinioResourceMapper {

    MinioResourceMapper INSTANCE = Mappers.getMapper(MinioResourceMapper.class);

    default ResourceResponseDto toResourceDto(StatObjectResponse response, Long userId) {
        if (response == null) {
            return null;
        }

        String relative = response.object().substring(PathUtils.getUserRootDirectoryPrefix(userId).length());

        boolean isDir = relative.endsWith("/.init");
        String trimmed = isDir
                ? relative.substring(0, relative.length() - 6)
                : relative;

        int lastSlash = trimmed.lastIndexOf("/");
        String name = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
        String path = lastSlash > 0 ? trimmed.substring(0, lastSlash + 1) : "/";

        return new ResourceResponseDto(path, name, response.size(), isDir ? "DIRECTORY" : "FILE");
    }

}
