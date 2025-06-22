package com.vladsv.cloud_file_storage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceResponseDto(String path, String name, Long size, String type) {

}
