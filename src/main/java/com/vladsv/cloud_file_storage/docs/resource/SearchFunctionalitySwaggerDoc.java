package com.vladsv.cloud_file_storage.docs.resource;

import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
        tags = {"Resource management"},
        summary = "Search resources",
        description = "Searches for files and folders by name.",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Resources found",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceResponseDto.class)))
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid or missing search query",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                ),
                @ApiResponse(
                        responseCode = "401",
                        description = "User is not authorized",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                ),
                @ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                )
        }
)
public @interface SearchFunctionalitySwaggerDoc {
}
