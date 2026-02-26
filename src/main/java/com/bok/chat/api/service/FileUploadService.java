package com.bok.chat.api.service;

import com.bok.chat.api.dto.FileUploadResponse;
import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.User;
import com.bok.chat.event.FileUploadedEvent;
import com.bok.chat.repository.FileAttachmentRepository;
import com.bok.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final long MAX_FILE_SIZE = 3 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FileUploadResponse upload(Long uploaderId, MultipartFile file) {
        validate(file);

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        FileAttachment attachment = FileAttachment.create(
                uploader,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        fileAttachmentRepository.save(attachment);

        try {
            String storedPath = fileStorageService.upload(
                    attachment.getId(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            );
            attachment.assignStoredPath(storedPath);
        } catch (IOException e) {
            throw new IllegalStateException("파일 읽기에 실패했습니다.", e);
        }

        eventPublisher.publishEvent(new FileUploadedEvent(attachment.getId()));

        return new FileUploadResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getThumbnailStatus()
        );
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }
        if (file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 3MB를 초과할 수 없습니다.");
        }
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
    }
}
