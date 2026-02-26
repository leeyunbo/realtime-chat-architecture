package com.bok.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false)
    private String originalFilename;

    private String storedPath;

    private String thumbnailPath;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThumbnailStatus thumbnailStatus;

    private FileAttachment(User uploader, String originalFilename, String contentType, long fileSize) {
        this.uploader = uploader;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.thumbnailStatus = isImage(contentType) ? ThumbnailStatus.PENDING : ThumbnailStatus.NONE;
    }

    public static FileAttachment create(User uploader, String originalFilename, String contentType, long fileSize) {
        return new FileAttachment(uploader, originalFilename, contentType, fileSize);
    }

    public void assignStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public void completeThumbnail(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
        this.thumbnailStatus = ThumbnailStatus.COMPLETED;
    }

    public void failThumbnail() {
        this.thumbnailStatus = ThumbnailStatus.FAILED;
    }

    private static boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    public enum ThumbnailStatus {
        PENDING, COMPLETED, FAILED, NONE
    }
}
