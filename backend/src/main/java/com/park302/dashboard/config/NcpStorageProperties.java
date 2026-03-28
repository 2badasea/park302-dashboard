package com.park302.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * NCP Object Storage 설정 프로퍼티
 * application.properties의 ncp.storage.* 값을 바인딩한다.
 * 실제 키/시크릿 값은 환경변수로 주입 — 코드/설정파일에 직접 기재 금지.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ncp.storage")
public class NcpStorageProperties {
    private String endpoint;
    private String regionName;
    private String bucketName;
    /** 버킷 내 루트 폴더 (dev/prod 환경 분리용) */
    private String rootDir;
    private String accessKey;
    private String secretKey;
}
