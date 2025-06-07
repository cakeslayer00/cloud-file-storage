package com.vladsv.cloud_file_storage.service;

import com.vladsv.cloud_file_storage.repository.MinioRepository;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioRepository minioRepository;

    public void downloadResource(String path, HttpServletResponse response) {
        if (!minioRepository.isDirectory(path)) {
            try (InputStream stream = minioRepository.getResource(path)) {
                StreamUtils.copy(stream, response.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            zipDirectoryContent(path, response);
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + path + "\"");
    }

    public StatObjectResponse manipulateResource(String from, String to) {
        minioRepository.copyResource(from, to);
        minioRepository.delete(from);
        return minioRepository.getResourceStat(to);
    }

    private void zipDirectoryContent(String path, HttpServletResponse response) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            List<String> resources = minioRepository.getAllResourceNames(path);

            resources.forEach(resource -> {
                try (InputStream in = minioRepository.getResource(resource)) {
                    String substring = resource.substring(path.length());
                    ZipEntry zipEntry = new ZipEntry(substring);
                    zipOutputStream.putNextEntry(zipEntry);
                    in.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            zipOutputStream.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
