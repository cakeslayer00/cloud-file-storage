package com.vladsv.cloud_file_storage.docs.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
        summary = "Download resource from storage",
        description = "Download resource from remote storage, from given path",
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Resource returned, if resource is a directory zip file with all contents returned",
                        content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
                )
        }
)
public @interface DownloadResourceSwaggerDoc {
}
