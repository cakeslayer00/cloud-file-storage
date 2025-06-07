package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.DirectoryResponseDto;
import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.mapper.MinioObjectMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.GenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/directory")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final MinioRepository minioRepository;

    @GetMapping
    public ResourceResponseDto getDirectoryContentInfo() {
        return null;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DirectoryResponseDto createEmptyDirectory(@RequestParam("path") String path) {
        GenericResponse response = minioRepository.commenceDirectory(path);

        return MinioObjectMapper.INSTANCE.toDirectoryDto(response);
    }

}
