package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.minio.GenericResponse;
import io.minio.StatObjectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MinioObjectMapper {

    MinioObjectMapper INSTANCE = Mappers.getMapper(MinioObjectMapper.class);

    default ResourceResponseDto toResourceDto(StatObjectResponse statObjectResponse) {
        if (statObjectResponse == null) {
            return null;
        }

        String name = getResourceName(statObjectResponse);
        String type = getResourceType(statObjectResponse);
        String path = getResourcePath(statObjectResponse);
        long size = statObjectResponse.size();

        return new ResourceResponseDto(path, name, size, type);
    }

    default String getResourcePath(StatObjectResponse response) {
        return response.object().substring(0, response.object().lastIndexOf("/") + 1);
    }

    default String getResourceType(StatObjectResponse response) {
        return response.object().endsWith("/.init") ? "DIRECTORY" : "FILE";
    }

    default String getResourceName(StatObjectResponse response) {
        return response.object().substring(response.object().lastIndexOf("/") + 1);
    }

}
