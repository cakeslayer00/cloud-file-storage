package com.vladsv.cloud_file_storage.controller;

import com.vladsv.cloud_file_storage.dto.ResourceResponseDto;
import com.vladsv.cloud_file_storage.entity.User;
import com.vladsv.cloud_file_storage.service.ResourceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/api/resource")
@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> get(@RequestParam("path") String path,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resourceService.getResourceStat(path, user.getId()));
    }

    @GetMapping("/download")
    public ResponseEntity<Void> download(@RequestParam("path") String path,
                                         @AuthenticationPrincipal User user,
                                         HttpServletResponse response) {
        resourceService.downloadResource(path, user.getId(), response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/move")
    public ResponseEntity<ResourceResponseDto> move(@RequestParam("from") String from,
                                                    @RequestParam("to") String to,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resourceService.moveOrRenameResource(from, to, user.getId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponseDto>> search(@RequestParam("query") String query,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resourceService.searchFromRoot(query, user.getId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponseDto> upload(@RequestParam("path") String path,
                                                      @RequestPart("file") MultipartFile file,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resourceService.uploadResource(path, file, user.getId()));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("path") String path,
                       @AuthenticationPrincipal User user) {
        resourceService.deleteResource(path, user.getId());
        return ResponseEntity.noContent().build();
    }

}
