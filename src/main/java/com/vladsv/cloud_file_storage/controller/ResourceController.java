package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.docs.StandardResourceApiResponses;
import com.vladsv.cloud_file_storage.dto.ErrorResponseDto;
import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/api/resource")
@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @Operation(
            tags = {"Resource management"},
            summary = "Get resource info",
            description = "Obtain resource metadata under given path",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Resource has been returned successfully",
                            content = @Content(schema = @Schema(implementation = ResourceResponseDto.class))
                    )
            }
    )
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ResourceResponseDto get(@RequestParam("path") String path,
                                   @AuthenticationPrincipal User user) {
        return resourceService.getResource(path, user.getId());
    }

    @Operation(
            tags = {"Resource management"},
            summary = "Delete resource",
            description = "Delete resource under given path",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Resource has been demolished successfully",
                            content = @Content(schema = @Schema(implementation = ResourceResponseDto.class))
                    )
            }
    )
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    public void delete(@RequestParam("path") String path,
                       @AuthenticationPrincipal User user) {
        resourceService.deleteResource(path, user.getId());
    }

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
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ResourceResponseDto> upload(@RequestParam("path") String path,
                                            @RequestPart("object") MultipartFile[] files,
                                            @AuthenticationPrincipal User user) {
        return resourceService.uploadResources(path, files, user.getId());
    }

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
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/download")
    public void download(@RequestParam("path") String path,
                         @AuthenticationPrincipal User user,
                         HttpServletResponse response) {
        resourceService.downloadResource(path, user.getId(), response);
    }

    @Operation(
            tags = {"Resource management"},
            summary = "Move or rename resource",
            description = "Move or rename resource, given source and target resource path",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "target file under new path or name",
                            content = @Content(schema = @Schema(implementation = ResourceResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Resource under target path already exists",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
                    )
            }
    )
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/move")
    public ResourceResponseDto move(@RequestParam("from") String from,
                                    @RequestParam("to") String to,
                                    @AuthenticationPrincipal User user) {
        return resourceService.moveOrRenameResource(from, to, user.getId());
    }

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
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/search")
    public List<ResourceResponseDto> search(@RequestParam("query") String query,
                                            @AuthenticationPrincipal User user) {
        return resourceService.search(query, user.getId());
    }

}
