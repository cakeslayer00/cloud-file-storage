package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import utils.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioRepository minioRepository;

    public StatObjectResponse moveResource(String from, String to) {
        minioRepository.copyResource(from, to);
        minioRepository.delete(from);
        return minioRepository.getResourceStat(to);
    }
}
