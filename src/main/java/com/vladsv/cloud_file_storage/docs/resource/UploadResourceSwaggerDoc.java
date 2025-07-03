package com.vladsv.cloud_file_storage.docs.resource;

import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Operation(
        tags = {"Resource management"},
        summary = "Upload resource",
        description = "Upload resource or resources under given path",
        requestBody = @RequestBody(
                required = true,
                content = @Content(
                        mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
                )
        ),
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Resource has been uploaded",
                        content = @Content(schema = @Schema(implementation = ResourceResponseDto.class))
                ),
                @ApiResponse(
                        responseCode = "409",
                        description = "Resource already exists",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                )
        }
)
public @interface UploadResourceSwaggerDoc {
}
