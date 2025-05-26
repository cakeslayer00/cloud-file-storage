package com.vladsv.cloud_file_storage.mapper;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.minio.StatObjectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MinioObjectMapper {

    MinioObjectMapper INSTANCE = Mappers.getMapper( MinioObjectMapper.class );

    //TODO: DO something about this, cuz looks like shit
    @Mapping(target = "name", expression = "java(statObjectResponse.object().substring(statObjectResponse.object().lastIndexOf(\"/\") + 1))")
    @Mapping(target = "type", expression = "java(statObjectResponse.object().endsWith(\"/\") ? \"DIRECTORY\" : \"FILE\")")
    @Mapping(target = "path", expression = "java(statObjectResponse.object().substring(0, statObjectResponse.object().lastIndexOf(\"/\") + 1))")
    ResourceResponseDto toDto(StatObjectResponse statObjectResponse);

}
