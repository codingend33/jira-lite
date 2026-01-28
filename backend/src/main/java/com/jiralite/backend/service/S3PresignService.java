package com.jiralite.backend.service;

import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Generates S3 presigned URLs for uploads and downloads.
 */
@Service
public class S3PresignService {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String bucket;
    private final Duration uploadExpiry;
    private final Duration downloadExpiry;

    public S3PresignService(
            S3Presigner presigner,
            S3Client s3Client,
            @Value("${app.s3.bucket}") String bucket,
            @Value("${app.s3.upload-expiry-seconds:300}") long uploadExpirySeconds,
            @Value("${app.s3.download-expiry-seconds:300}") long downloadExpirySeconds) {
        this.presigner = presigner;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.uploadExpiry = Duration.ofSeconds(uploadExpirySeconds);
        this.downloadExpiry = Duration.ofSeconds(downloadExpirySeconds);
    }

    public PresignResult presignUpload(String key, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putRequest)
                .signatureDuration(uploadExpiry)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        Map<String, String> headers = new HashMap<>();
        presigned.signedHeaders().forEach((headerName, values) -> headers.put(headerName, String.join(",", values)));
        return new PresignResult(presigned.url(), headers, OffsetDateTime.now().plusSeconds(uploadExpiry.getSeconds()));
    }

    public PresignResult presignDownload(String key, String fileName, String contentType) {
        GetObjectRequest.Builder getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        if (fileName != null && !fileName.isBlank()) {
            getRequest.responseContentDisposition(buildContentDisposition(fileName));
        }
        if (contentType != null && !contentType.isBlank()) {
            getRequest.responseContentType(contentType);
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getRequest.build())
                .signatureDuration(downloadExpiry)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        return new PresignResult(presigned.url(), Map.of(),
                OffsetDateTime.now().plusSeconds(downloadExpiry.getSeconds()));
    }

    public void deleteObject(String key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    private String buildContentDisposition(String fileName) {
        String safe = fileName.replaceAll("[\\r\\n\"]", "_");
        return "attachment; filename=\"" + safe + "\"";
    }

    public record PresignResult(URL url, Map<String, String> headers, OffsetDateTime expiresAt) {
        public Map<String, String> headersOrEmpty() {
            return headers == null ? Map.of() : new HashMap<>(headers);
        }
    }
}
