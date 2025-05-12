package com.vladsv.cloud_file_storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequestDto(@NotBlank @Size(min = 4, message = "username length must be more than 4 characters")
                             String username,
                             @NotBlank @Size(min = 8, max = 255, message = "password length must be more than 8 characters")
                             String password) {
}
