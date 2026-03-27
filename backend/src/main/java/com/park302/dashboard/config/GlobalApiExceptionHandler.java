package com.park302.dashboard.config;

import com.park302.dashboard.common.ResMessage;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 API 예외 처리
 * 컨트롤러 계층에서 발생하는 예외를 잡아 ResMessage 형태로 반환한다.
 *
 * 주의: 컨트롤러 실행 이전(예: multipart 파싱, 필터 단계)에 던져지는 예외는
 * @RestControllerAdvice가 잡지 못하므로 별도 처리가 필요할 수 있다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    /**
     * 리소스를 찾을 수 없는 경우 (404)
     * 예: agentRepository.findById() 실패 시 서비스에서 throw
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResMessage<Void>> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("EntityNotFound: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ResMessage<>(-1, e.getMessage(), null));
    }

    /**
     * @Valid 검증 실패 (400)
     * 필드별 오류 메시지를 콤마로 합쳐 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResMessage<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", msg);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ResMessage<>(-1, msg, null));
    }

    /**
     * 처리되지 않은 모든 예외 (500)
     * 스택트레이스는 로그에만 남기고, 클라이언트에는 내부 정보를 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResMessage<Void>> handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ResMessage<>(-1, "서버 오류가 발생했습니다.", null));
    }
}
