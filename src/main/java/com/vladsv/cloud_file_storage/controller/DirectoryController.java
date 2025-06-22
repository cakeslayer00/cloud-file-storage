package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.service.DirectoryService;
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

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<ResourceResponseDto> getDirectoryContent(@RequestParam("path") String path,
                                                                         @AuthenticationPrincipal User user) {
        return directoryService.getDirectoryContent(path, user.getId());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResourceResponseDto createDirectory(@RequestParam("path") String path,
                                                               @AuthenticationPrincipal User user) {
        return directoryService.createEmptyDirectory(path, user.getId());
    }

}
