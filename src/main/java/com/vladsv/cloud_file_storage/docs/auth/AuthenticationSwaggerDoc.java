package com.vladsv.cloud_file_storage.docs.auth;

import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Operation(
        tags = {"Authorization"},
        summary = "Authenticate user",
        description = "Authenticates existing user and returns basic user information",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "User authenticated",
                        content = @Content(schema = @Schema(implementation = UserResponseDto.class))
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Validation error",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                ),
                @ApiResponse(
                        responseCode = "401",
                        description = "Invalid input data",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                ),
                @ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                )
        }
)
public @interface AuthenticationSwaggerDoc {
}
