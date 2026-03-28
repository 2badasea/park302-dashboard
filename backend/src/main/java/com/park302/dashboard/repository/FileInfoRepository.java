package com.park302.dashboard.repository;

import com.park302.dashboard.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {

    /** 특정 도메인 레코드에 첨부된 파일 목록 조회 */
    List<FileInfo> findByRefTableAndRefId(String refTable, Long refId);

    /**
     * 특정 도메인 레코드의 특정 파일 조회
     * 다운로드 시 파일이 해당 work에 속하는지 검증하는 데 사용
     */
    Optional<FileInfo> findByIdAndRefTableAndRefId(Long id, String refTable, Long refId);
}
