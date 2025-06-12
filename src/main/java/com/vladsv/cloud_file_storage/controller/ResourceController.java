package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.service.DirectoryService;
import com.vladsv.cloud_file_storage.service.ResourceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RequestMapping("/api/resource")
@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponseDto get(@RequestParam("path") String path,
                                   @AuthenticationPrincipal User user) {
        return resourceService.getResourceStat(path, user.getId());
    }

    @GetMapping("/download")
    @ResponseStatus(HttpStatus.OK)
    public void download(@RequestParam("path") String path,
                         @AuthenticationPrincipal User user,
                         HttpServletResponse response) {
        resourceService.downloadResource(path, user.getId(), response);
    }


    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam("path") String path,
                       @AuthenticationPrincipal User user) {
        resourceService.deleteResource(path, user.getId());
    }

}
