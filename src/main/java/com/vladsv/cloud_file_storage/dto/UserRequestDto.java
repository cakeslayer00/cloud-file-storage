package com.vladsv.cloud_file_storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDto(@NotBlank String username, @NotBlank @Size(min = 8, max = 255) String password) {
}
