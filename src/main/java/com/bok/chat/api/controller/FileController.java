package com.bok.chat.api.controller;

import com.bok.chat.api.dto.FileBatchDownloadResponse;
import com.bok.chat.api.dto.FileDownloadResponse;
import com.bok.chat.api.dto.FileUploadResponse;
import com.bok.chat.api.service.FileDownloadService;
import com.bok.chat.api.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileDownloadService fileDownloadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(fileUploadService.upload(userId, file));
    }

    @GetMapping("/{fileId}/download-url")
    public ResponseEntity<FileDownloadResponse> getDownloadUrl(
            Authentication authentication,
            @PathVariable Long fileId) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(fileDownloadService.getDownloadUrl(userId, fileId));
    }

    @GetMapping("/{fileId}/thumbnail-url")
    public ResponseEntity<FileDownloadResponse> getThumbnailUrl(
            Authentication authentication,
            @PathVariable Long fileId) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(fileDownloadService.getThumbnailUrl(userId, fileId));
    }

    @GetMapping("/download-urls")
    public ResponseEntity<List<FileBatchDownloadResponse>> getBatchDownloadUrls(
            Authentication authentication,
            @RequestParam List<Long> fileIds) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(fileDownloadService.getBatchDownloadUrls(userId, fileIds));
    }
}
