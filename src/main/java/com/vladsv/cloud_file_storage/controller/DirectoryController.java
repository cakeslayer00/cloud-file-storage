package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.exception.FolderAlreadyExistsException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

@RequestMapping("/api/directory")
@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private static final String COMMON_BUCKET = "user-files";

    private final MinioClient minioClient;

    //TODO: Some wild shit, haven't tested, need more info
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponseDto createEmptyDirectory(@RequestParam("path") String path) {
        try {
            if (isObjectExist(path)) {
                throw new FolderAlreadyExistsException(path);
            }

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(COMMON_BUCKET).object(path).stream(
                                    new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());


        } catch (MinioException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        return new ResourceResponseDto("sex", "drugs", ".etc", "robots");
    }

    private boolean isObjectExist(String name) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(COMMON_BUCKET)
                    .object(name).build());
            return true;
        } catch (ErrorResponseException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

}
