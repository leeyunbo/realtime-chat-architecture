package com.bok.chat.api.service;

import com.bok.chat.config.S3Config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Config s3Config;
    private final S3KeyGenerator s3KeyGenerator;

    public String upload(Long fileId, String originalFilename, String contentType, byte[] data) {
        String key = s3KeyGenerator.buildKey(fileId, originalFilename);
        putObject(key, contentType, data);
        return key;
    }

    public void uploadWithKey(String key, String contentType, byte[] data) {
        putObject(key, contentType, data);
    }

    private void putObject(String key, String contentType, byte[] data) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
    }

    public byte[] download(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }
}
