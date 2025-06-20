package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.service.DirectoryService;
import com.vladsv.cloud_file_storage.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import utils.PathUtils;

import java.util.List;

@RequestMapping("/api/directory")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getDirectoryContent(@RequestParam("path") String path,
                                                                         @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(directoryService.getDirResources(path, user.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ResourceResponseDto> createDirectory(@RequestParam("path") String path,
                                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(directoryService.createEmptyDirectory(path, user.getId()));
    }

}
