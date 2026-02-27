package com.bok.chat.api.service;

import com.bok.chat.api.dto.FileBatchDownloadResponse;
import com.bok.chat.api.dto.FileDownloadResponse;
import com.bok.chat.entity.FileAttachment;
import com.bok.chat.entity.FileAttachment.ThumbnailStatus;
import com.bok.chat.repository.ChatRoomUserRepository;
import com.bok.chat.repository.FileAttachmentRepository;
import com.bok.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileDownloadService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final FileStorageService fileStorageService;
    private final S3KeyGenerator s3KeyGenerator;

    public FileDownloadResponse getDownloadUrl(Long userId, Long fileId) {
        FileAttachment file = findFileAndValidateAccess(userId, fileId);

        String key = s3KeyGenerator.buildKey(file.getId(), file.getOriginalFilename());
        String url = fileStorageService.generatePresignedUrl(key);

        return new FileDownloadResponse(url, file.getOriginalFilename(),
                file.getContentType(), file.getFileSize());
    }

    public FileDownloadResponse getThumbnailUrl(Long userId, Long fileId) {
        FileAttachment file = findFileAndValidateAccess(userId, fileId);

        if (file.getThumbnailStatus() != ThumbnailStatus.COMPLETED) {
            throw new IllegalArgumentException("썸네일이 존재하지 않습니다.");
        }

        String key = s3KeyGenerator.buildThumbnailKey(file.getId(), file.getOriginalFilename());
        String url = fileStorageService.generatePresignedUrl(key);

        return new FileDownloadResponse(url, file.getOriginalFilename(),
                file.getContentType(), file.getFileSize());
    }

    public List<FileBatchDownloadResponse> getBatchDownloadUrls(Long userId, List<Long> fileIds) {
        List<FileAttachment> files = fileAttachmentRepository.findAllById(fileIds);

        if (files.size() != fileIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 파일이 포함되어 있습니다.");
        }

        List<Object[]> chatRoomMappings = messageRepository.findChatRoomIdsByFileIds(fileIds);
        Map<Long, Long> fileIdToChatRoomId = chatRoomMappings.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        if (fileIdToChatRoomId.size() != fileIds.size()) {
            throw new IllegalArgumentException("메시지에 연결되지 않은 파일이 포함되어 있습니다.");
        }

        for (Long chatRoomId : fileIdToChatRoomId.values().stream().distinct().toList()) {
            chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("접근 권한이 없는 파일이 포함되어 있습니다."));
        }

        return files.stream()
                .map(file -> {
                    String downloadKey = s3KeyGenerator.buildKey(file.getId(), file.getOriginalFilename());
                    String downloadUrl = fileStorageService.generatePresignedUrl(downloadKey);

                    String thumbnailUrl = null;
                    if (file.getThumbnailStatus() == ThumbnailStatus.COMPLETED) {
                        String thumbnailKey = s3KeyGenerator.buildThumbnailKey(file.getId(), file.getOriginalFilename());
                        thumbnailUrl = fileStorageService.generatePresignedUrl(thumbnailKey);
                    }

                    return new FileBatchDownloadResponse(file.getId(), downloadUrl, thumbnailUrl,
                            file.getOriginalFilename(), file.getContentType(), file.getFileSize());
                })
                .toList();
    }

    private FileAttachment findFileAndValidateAccess(Long userId, Long fileId) {
        FileAttachment file = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다."));

        Long chatRoomId = messageRepository.findChatRoomIdByFileId(fileId)
                .orElseThrow(() -> new IllegalArgumentException("메시지에 연결되지 않은 파일입니다."));

        chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 파일에 접근 권한이 없습니다."));

        return file;
    }
}
