package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.docs.StandardResourceApiResponses;
import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.service.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/directory")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

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
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<ResourceResponseDto> getDirectoryContent(@RequestParam("path") String path,
                                                         @AuthenticationPrincipal User user) {
        return directoryService.getDirectoryContent(path, user.getId());
    }

    @Operation(
            tags = {"Directory management"},
            summary = "Create empty directory",
            description = "Creates an empty directory at the given path",
            parameters = {
                    @Parameter(
                            name = "path",
                            description = "Path to directory",
                            required = true,
                            style = ParameterStyle.FORM,
                            explode = Explode.FALSE
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Directory created",
                            content = @Content(schema = @Schema(implementation = ResourceResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Directory already exists",
                            content =  @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    )
            }
    )
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResourceResponseDto createDirectory(@RequestParam("path") String path,
                                               @AuthenticationPrincipal User user) {
        return directoryService.createEmptyDirectory(path, user.getId());
    }

}
