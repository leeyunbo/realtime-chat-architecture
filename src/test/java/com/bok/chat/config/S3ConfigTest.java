package com.bok.chat.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("S3Config 통합 테스트")
class S3ConfigTest {

    private static final String BUCKET = "test-bucket";
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";

    @Container
    static final MinIOContainer minio = new MinIOContainer("minio/minio:latest")
            .withUserName(ACCESS_KEY)
            .withPassword(SECRET_KEY);

    private static S3Client s3Client;
    private static S3Presigner s3Presigner;

    @BeforeAll
    static void setUp() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));

        URI endpoint = URI.create(minio.getS3URL());

        s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .forcePathStyle(true)
                .build();

        s3Presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    }

    @Test
    @DisplayName("파일 업로드 → 다운로드 → 삭제 라운드트립")
    void uploadDownloadDelete_roundTrip() {
        String key = "test/hello.txt";
        String content = "Hello, MinIO!";

        s3Client.putObject(
                PutObjectRequest.builder().bucket(BUCKET).key(key).build(),
                RequestBody.fromString(content));

        byte[] downloaded = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()).asByteArray();
        assertThat(new String(downloaded, StandardCharsets.UTF_8)).isEqualTo(content);

        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(BUCKET).key(key).build());

        assertThat(listObjectKeys()).doesNotContain(key);
    }

    @Test
    @DisplayName("Presigned URL 생성")
    void presignedUrl_shouldBeGenerated() {
        String key = "test/presign.txt";

        s3Client.putObject(
                PutObjectRequest.builder().bucket(BUCKET).key(key).build(),
                RequestBody.fromString("presigned content"));

        String presignedUrl = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(BUCKET)
                                .key(key)
                                .build())
                        .build()).url().toString();

        assertThat(presignedUrl).contains(BUCKET).contains(key);

        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(BUCKET).key(key).build());
    }

    @Test
    @DisplayName("버킷 자동 생성 확인")
    void createBucketIfNotExists() {
        String newBucket = "auto-created-bucket";

        s3Client.createBucket(CreateBucketRequest.builder().bucket(newBucket).build());

        HeadBucketResponse response = s3Client.headBucket(
                HeadBucketRequest.builder().bucket(newBucket).build());
        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();

        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(newBucket).build());
    }

    private java.util.List<String> listObjectKeys() {
        return s3Client.listObjectsV2(
                        ListObjectsV2Request.builder().bucket(BUCKET).build())
                .contents().stream()
                .map(S3Object::key)
                .toList();
    }
}
