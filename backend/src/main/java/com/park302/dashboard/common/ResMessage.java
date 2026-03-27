package com.park302.dashboard.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공통 API 응답 래퍼
 * - code:  0 = 기본값(미설정), 1 = 정상 처리, -1 = 오류 (예외/내부 오류)
 * - msg:   사용자에게 전달할 메시지 (성공 안내 또는 오류 안내)
 * - data:  단건 객체 또는 PageData<T> 등 페이지 응답. 오류 시 null
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResMessage<T> {
    private int code = 0;
    private String msg = "";
    private T data;
}
