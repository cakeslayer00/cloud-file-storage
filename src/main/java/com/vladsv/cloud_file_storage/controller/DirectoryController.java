package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/directory")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final MinioRepository minioRepository;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponseDto> getDirectoryContent(@RequestParam("path") String path, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return minioRepository.getDirectoryResources(path, user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<ResourceResponseDto> createDirectory(@RequestParam("path") String path, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        minioRepository.createDirectory(path, user.getId());
        return minioRepository.getDirectoryResources(path, user.getId());
    }

}
