package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.mapper.MinioObjectMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/resource")
@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final MinioRepository minioRepository;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponseDto getResource(@RequestParam("path") String path) {
        StatObjectResponse object = minioRepository.getObject(path);

        ResourceResponseDto resource = MinioObjectMapper.INSTANCE.toDto(object);

        return resource;
    }

}
