package com.bok.chat.api.service;

import com.bok.chat.api.dto.FileBatchDownloadResponse;
import com.bok.chat.api.dto.FileDownloadResponse;
import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.FileAttachment.ThumbnailStatus;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileDownloadService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final FileStorageService fileStorageService;
    private final S3KeyGenerator s3KeyGenerator;

    public FileDownloadResponse getDownloadUrl(Long userId, Long fileId) {
        FileAttachment file = findFileAndValidateAccess(userId, fileId);

        return new FileDownloadResponse(generateDownloadUrl(file), file.getOriginalFilename(),
                file.getContentType(), file.getFileSize());
    }

    public FileDownloadResponse getThumbnailUrl(Long userId, Long fileId) {
        FileAttachment file = findFileAndValidateAccess(userId, fileId);

        if (file.getThumbnailStatus() != ThumbnailStatus.COMPLETED) {
            throw new IllegalArgumentException("썸네일이 존재하지 않습니다.");
        }

        return new FileDownloadResponse(generateThumbnailUrl(file), file.getOriginalFilename(),
                file.getContentType(), file.getFileSize());
    }

    public List<FileBatchDownloadResponse> getBatchDownloadUrls(Long userId, List<Long> fileIds) {
        List<FileAttachment> files = fileAttachmentRepository.findAllById(fileIds);

        if (files.size() != fileIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 파일이 포함되어 있습니다.");
        }

        for (Long chatRoomId : files.stream().map(f -> f.getChatRoom().getId()).distinct().toList()) {
            chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("접근 권한이 없는 파일이 포함되어 있습니다."));
        }

        return files.stream()
                .map(file -> {
                    String thumbnailUrl = file.getThumbnailStatus() == ThumbnailStatus.COMPLETED
                            ? generateThumbnailUrl(file) : null;

                    return new FileBatchDownloadResponse(file.getId(), generateDownloadUrl(file), thumbnailUrl,
                            file.getOriginalFilename(), file.getContentType(), file.getFileSize());
                })
                .toList();
    }

    private FileAttachment findFileAndValidateAccess(Long userId, Long fileId) {
        FileAttachment file = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다."));

        chatRoomUserRepository.findByChatRoomIdAndUserId(file.getChatRoom().getId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 파일에 접근 권한이 없습니다."));

        return file;
    }

    private String generateDownloadUrl(FileAttachment file) {
        String key = s3KeyGenerator.buildKey(file.getId(), file.getOriginalFilename());
        return fileStorageService.generatePresignedUrl(key);
    }

    private String generateThumbnailUrl(FileAttachment file) {
        String key = s3KeyGenerator.buildThumbnailKey(file.getId(), file.getOriginalFilename());
        return fileStorageService.generatePresignedUrl(key);
    }
}
