package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.docs.*;
import com.vladsv.cloud_file_storage.docs.resource.*;
import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.service.ResourceService;
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

    @ObtainResourceSwaggerDoc
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public ResourceResponseDto get(@RequestParam("path") String path,
                                   @AuthenticationPrincipal User user) {
        return resourceService.getResource(path, user.getId());
    }

    @DeleteResourceSwaggerDoc
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    public void delete(@RequestParam("path") String path,
                       @AuthenticationPrincipal User user) {
        resourceService.deleteResource(path, user.getId());
    }

    @UploadResourceSwaggerDoc
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ResourceResponseDto> upload(@RequestParam("path") String path,
                                            @RequestPart("object") List<MultipartFile> files,
                                            @AuthenticationPrincipal User user) {
        return resourceService.uploadResources(path, files, user.getId());
    }

    @DownloadResourceSwaggerDoc
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/download")
    public void download(@RequestParam("path") String path,
                         @AuthenticationPrincipal User user,
                         HttpServletResponse response) {
        resourceService.downloadResource(path, user.getId(), response);
    }

    @ResourceManipulationSwaggerDoc
    @StandardResourceApiResponses
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/move")
    public ResourceResponseDto move(@RequestParam("from") String from,
                                    @RequestParam("to") String to,
                                    @AuthenticationPrincipal User user) {
        return resourceService.moveOrRenameResource(from, to, user.getId());
    }

    @SearchFunctionalitySwaggerDoc
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/search")
    public List<ResourceResponseDto> search(@RequestParam("query") String query,
                                            @AuthenticationPrincipal User user) {
        return resourceService.search(query, user.getId());
    }

}
