package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.minio.StatObjectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MinioResourceMapper {

    MinioResourceMapper INSTANCE = Mappers.getMapper(MinioResourceMapper.class);

    default ResourceResponseDto toResourceDto(StatObjectResponse response) {
        if (response == null) {
            return null;
        }

        String relative = response.object();

        boolean isDir = relative.endsWith("/.init");
        String trimmed = isDir
                ? relative.substring(0, relative.length() - 6)
                : relative;

        int firstSlash = trimmed.indexOf('/');
        int lastSlash = trimmed.lastIndexOf("/");
        String path = lastSlash > 0 ? trimmed.substring(firstSlash + 1, lastSlash + 1) : "/";
        String name = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;

        return new ResourceResponseDto(path.isEmpty() ? "/" : path, name, response.size(), isDir ? "DIRECTORY" : "FILE");
    }

}
