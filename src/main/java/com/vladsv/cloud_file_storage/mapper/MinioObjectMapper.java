package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
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

        String name = getObjectName(statObjectResponse);
        String type = getObjectType(statObjectResponse);
        String path = getObjectPath(statObjectResponse);
        long size = statObjectResponse.size();

        return new ResourceResponseDto(path, name, size, type);
    }

    default String getObjectPath(StatObjectResponse statObjectResponse) {
        return statObjectResponse.object().substring(0, statObjectResponse.object().lastIndexOf("/") + 1);
    }

    default String getObjectType(StatObjectResponse statObjectResponse) {
        return statObjectResponse.object().endsWith("/") ? "DIRECTORY" : "FILE";
    }

    default String getObjectName(StatObjectResponse statObjectResponse) {
        return statObjectResponse.object().substring(statObjectResponse.object().lastIndexOf("/") + 1);
    }

}
