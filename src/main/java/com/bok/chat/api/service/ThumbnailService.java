package com.bok.chat.api.service;

import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.FileAttachment.ThumbnailStatus;
import com.bok.chat.event.FileUploadedEvent;
import com.bok.chat.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileStorageService fileStorageService;
    private final S3KeyGenerator s3KeyGenerator;
    private final ImageResizer imageResizer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFileUploaded(FileUploadedEvent event) {
        FileAttachment file = fileAttachmentRepository.findById(event.fileId())
                .orElse(null);

        if (file == null || file.getThumbnailStatus() != ThumbnailStatus.PENDING) {
            return;
        }

        try {
            String thumbnailKey = generateAndUploadThumbnail(file);
            file.completeThumbnail(thumbnailKey);
        } catch (Exception e) {
            log.error("Failed to generate thumbnail for file {}", event.fileId(), e);
            file.failThumbnail();
        }
    }

    private String generateAndUploadThumbnail(FileAttachment file) throws IOException {
        byte[] originalData = fileStorageService.download(file.getStoredPath());
        byte[] thumbnailData = imageResizer.resize(originalData, file.getContentType());

        String thumbnailKey = s3KeyGenerator.buildThumbnailKey(file.getId(), file.getOriginalFilename());
        fileStorageService.uploadWithKey(thumbnailKey, file.getContentType(), thumbnailData);

        return thumbnailKey;
    }
}
