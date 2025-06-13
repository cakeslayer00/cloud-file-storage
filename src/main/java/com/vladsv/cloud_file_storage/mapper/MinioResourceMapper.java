package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import utils.PathUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Mapper
public interface MinioResourceMapper {

    MinioResourceMapper INSTANCE = Mappers.getMapper(MinioResourceMapper.class);

    default ResourceResponseDto toResourceDto(Result<Item> result, Long id) {
        try {
            if (result == null) {
                return null;
            }

            Item item = result.get();

            String relative = item.objectName().substring(PathUtils.getUserRootDirectoryPrefix(id).length());

            boolean isDir = relative.endsWith("/.init");
            String trimmed = isDir
                    ? relative.substring(0, relative.length() - 6)
                    : relative;

            int lastSlash = trimmed.lastIndexOf("/");
            String name = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
            String path = lastSlash > 0 ? trimmed.substring(0, lastSlash + 1) : "/";

            return new ResourceResponseDto(path, name, item.size(), isDir ? "DIRECTORY" : "FILE");
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    default ResourceResponseDto toResourceDto(StatObjectResponse response, Long id) {
        if (response == null) {
            return null;
        }

        String relative = response.object().substring(PathUtils.getUserRootDirectoryPrefix(id).length());

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
