package com.park302.dashboard.service;

import com.park302.dashboard.config.NcpStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

/**
 * NCP Object Storage 파일 업로드/다운로드/삭제 서비스
 * AWS SDK v2 S3 API를 사용하며 NCP endpoint로 오버라이드된 S3Client를 주입받는다.
 *
 * 스토리지 경로 규칙: {rootDir}/{clientCode}/{domainTable}/{uuid}_{originalName}
 * 예) dev/cali/work/550e8400-e29b-..._report.pdf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NcpStorageService {

    private final S3Client s3Client;
    private final NcpStorageProperties properties;

    /**
     * 파일 업로드
     * @param clientCode  업체 식별 코드 (경로 분리용)
     * @param domainTable 도메인 테이블명 (예: "work")
     * @param file        업로드할 MultipartFile
     * @return UploadResult — storagePath와 메타데이터를 file_info 저장에 사용
     */
    public UploadResult upload(String clientCode, String domainTable, MultipartFile file)
        throws IOException {

        String originalName = file.getOriginalFilename() != null
            ? file.getOriginalFilename() : "unknown";
        String storedName = UUID.randomUUID() + "_" + originalName;
        // {rootDir}/{clientCode}/{domainTable}/{uuid}_{originalName}
        String storagePath = String.join("/",
            properties.getRootDir(), clientCode, domainTable, storedName);

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(properties.getBucketName())
            .key(storagePath)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.debug("NCP upload success: {}", storagePath);

        return new UploadResult(originalName, storedName, storagePath, file.getSize(), file.getContentType());
    }

    /**
     * 파일 다운로드 스트림 반환
     * 호출부에서 try-with-resources로 스트림을 닫아야 한다.
     *
     * TODO: 대용량 파일이나 트래픽이 많아지면 pre-signed URL 방식으로 전환 권장.
     *       S3Presigner.presignGetObject()를 사용하여 302 리다이렉트로 처리하면
     *       서버 메모리 부담 없이 클라이언트가 NCP에 직접 다운로드 가능.
     *
     * @param storagePath file_info.storage_path 값
     */
    public ResponseInputStream<GetObjectResponse> download(String storagePath) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(properties.getBucketName())
            .key(storagePath)
            .build();
        return s3Client.getObject(request);
    }

    /**
     * 파일 삭제
     * @param storagePath file_info.storage_path 값
     */
    public void delete(String storagePath) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(properties.getBucketName())
            .key(storagePath)
            .build();
        s3Client.deleteObject(request);
        log.debug("NCP delete success: {}", storagePath);
    }

    /** 업로드 결과 DTO — file_info 테이블 저장 시 사용 */
    public record UploadResult(
        String originalName,
        String storedName,
        String storagePath,
        long fileSize,
        String mimeType
    ) {}
}
