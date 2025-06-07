package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.DirectoryResponseDto;
import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.minio.GenericResponse;
import io.minio.StatObjectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MinioObjectMapper {

    MinioObjectMapper INSTANCE = Mappers.getMapper(MinioObjectMapper.class);

    default DirectoryResponseDto toDirectoryDto(GenericResponse genericResponse) {
        if (genericResponse == null) {
            return null;
        }

        String name = getDirectoryName(genericResponse);
        String type = getObjectType(genericResponse);
        String path = getDirectoryPath(genericResponse);

        return new DirectoryResponseDto(path, name, type);
    }

    default ResourceResponseDto toResourceDto(StatObjectResponse statObjectResponse) {
        if (statObjectResponse == null) {
            return null;
        }

        String name = getResourceName(statObjectResponse);
        String type = getObjectType(statObjectResponse);
        String path = getObjectPath(statObjectResponse);
        long size = statObjectResponse.size();

        return new ResourceResponseDto(path, name, size, type);
    }

    default String getDirectoryPath(GenericResponse response) {
        return "/"; //TODO: for now leave it this way, need to implement allocation of bucket based on user id
    }

    default String getDirectoryName(GenericResponse response) {
        return response.object().substring(0, response.object().lastIndexOf("/") + 1);
    }

    default String getObjectPath(GenericResponse response) {
        return response.object().substring(0, response.object().lastIndexOf("/") + 1);
    }

    default String getObjectType(GenericResponse response) {
        return response.object().endsWith("/.init") ? "DIRECTORY" : "FILE";
    }

    default String getResourceName(GenericResponse response) {
        return response.object().substring(response.object().lastIndexOf("/") + 1);
    }

}
