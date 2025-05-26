package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/directory")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final MinioRepository minioRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponseDto createEmptyDirectory(@RequestParam("path") String path) {
        minioRepository.createEmptyFolder(path);


        return new ResourceResponseDto("sex", "drugs", 0, "robots");
    }

}
