package com.vladsv.cloud_file_storage.docs.directory;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
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
        tags = {"Directory management"},
        summary = "Returns directory contents",
        description = "Returns collection of resources from given path directory",
        parameters = {
                @Parameter(
                        name = "path",
                        description = "Path to directory, empty string means root",
                        required = true,
                        allowEmptyValue = true,
                        style = ParameterStyle.FORM,
                        explode = Explode.FALSE
                )
        },
        responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Collection of resources returned",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceResponseDto.class)))
                )
        }
)
public @interface GetDirectoryContentSwaggerDoc {
}
