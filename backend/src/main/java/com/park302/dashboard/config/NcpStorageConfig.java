package com.park302.dashboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * NCP Object Storage S3 클라이언트 설정
 * NCP는 S3 호환 API를 제공하므로 AWS SDK v2를 사용하되 endpoint만 NCP로 오버라이드한다.
 */
@Configuration
@EnableConfigurationProperties(NcpStorageProperties.class)
@RequiredArgsConstructor
public class NcpStorageConfig {

    private final NcpStorageProperties properties;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            // NCP Object Storage endpoint — AWS가 아닌 NCP로 오버라이드
            .endpointOverride(URI.create(properties.getEndpoint()))
            .region(Region.of(properties.getRegionName()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
            ))
            .build();
    }
}
