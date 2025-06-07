package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.mapper.MinioObjectMapper;
import com.vladsv.cloud_file_storage.repository.MinioRepository;
import com.vladsv.cloud_file_storage.service.MinioService;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/resource")
@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final MinioRepository minioRepository;
    private final MinioService minioService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponseDto getResource(@RequestParam("path") String path) {
        StatObjectResponse object = minioRepository.getResourceStat(path);

        return MinioObjectMapper.INSTANCE.toResourceDto(object);
    }

    @GetMapping("/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponseDto moveOrRenameResource(@RequestParam("from") String from, @RequestParam("to") String to) {
        StatObjectResponse object = minioService.manipulateResource(from, to);

        return MinioObjectMapper.INSTANCE.toResourceDto(object);
    }

    @GetMapping("/download")
    @ResponseStatus(HttpStatus.OK)
    public void downloadResource(@RequestParam("path") String path, HttpServletResponse response) {
        minioService.downloadResource(path, response);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam("path") String path) {
        minioRepository.delete(path);
    }

}
