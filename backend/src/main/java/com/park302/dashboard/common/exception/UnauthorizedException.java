package com.park302.dashboard.common.exception;

/** 인증 실패 예외 (API 키 없음/불일치, JWT 없음/만료 등) */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
