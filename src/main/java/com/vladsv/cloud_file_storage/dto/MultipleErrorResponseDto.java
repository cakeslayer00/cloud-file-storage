package com.vladsv.cloud_file_storage.dto;

import java.util.List;

public record MultipleErrorResponseDto(List<String> errors) {
}
