package com.park302.dashboard.dto;

import java.time.LocalDateTime;

/** 첨부파일 응답 DTO */
public class FileInfoDTO {

    /**
     * 파일 목록/상세 응답 항목
     * storagePath, storedName 등 내부 경로는 포함하지 않는다.
     */
    public record Item(
        Long id,
        String originalName,
        String fileExt,
        Long fileSize,
        String mimeType,
        LocalDateTime createdAt
    ) {}
}
