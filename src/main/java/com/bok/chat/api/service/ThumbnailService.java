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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private static final int MAX_THUMBNAIL_SIZE = 200;

    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileStorageService fileStorageService;
    private final S3KeyGenerator s3KeyGenerator;

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
            byte[] originalData = fileStorageService.download(file.getStoredPath());
            byte[] thumbnailData = resize(originalData, file.getContentType());

            String thumbnailKey = s3KeyGenerator.buildThumbnailKey(file.getId(), file.getOriginalFilename());
            fileStorageService.uploadWithKey(thumbnailKey, file.getContentType(), thumbnailData);

            file.completeThumbnail(thumbnailKey);
        } catch (Exception e) {
            log.error("Failed to generate thumbnail for file {}", event.fileId(), e);
            file.failThumbnail();
        }
    }

    byte[] resize(byte[] imageData, String contentType) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageData));
        if (original == null) {
            throw new IOException("Failed to read image");
        }

        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        if (originalWidth <= MAX_THUMBNAIL_SIZE && originalHeight <= MAX_THUMBNAIL_SIZE) {
            return imageData;
        }

        double scale = Math.min(
                (double) MAX_THUMBNAIL_SIZE / originalWidth,
                (double) MAX_THUMBNAIL_SIZE / originalHeight
        );

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, original.getType() != 0 ? original.getType() : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        String formatName = extractFormatName(contentType);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, formatName, out);
        return out.toByteArray();
    }

    private String extractFormatName(String contentType) {
        if (contentType == null) {
            return "png";
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "png";
        };
    }
}
