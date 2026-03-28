package com.park302.dashboard.controller;

import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.config.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 인증 컨트롤러
 * 단일 관리자 전용 — DB 조회 없이 환경변수(ADMIN_USERNAME, ADMIN_PASSWORD_HASH)와 비교한다.
 * 비밀번호는 BCrypt 해시값으로 저장하고 PasswordEncoder.matches()로 검증한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    /** BCrypt 해시값 — 평문 비밀번호 저장 금지 */
    @Value("${admin.password-hash}")
    private String adminPasswordHash;

    /**
     * POST /api/auth/login
     * Request : { "username": "admin", "password": "plain_password" }
     * Response: { code: 1, data: { "token": "eyJ..." } }
     */
    @PostMapping("/login")
    public ResponseEntity<ResMessage<TokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        // username 일치 + BCrypt 해시 비교
        if (!adminUsername.equals(req.getUsername())
            || !passwordEncoder.matches(req.getPassword(), adminPasswordHash)) {
            return ResponseEntity.status(401)
                .body(new ResMessage<>(-1, "아이디 또는 비밀번호가 올바르지 않습니다.", null));
        }

        String token = jwtUtil.generate(req.getUsername());
        return ResponseEntity.ok(new ResMessage<>(1, "로그인 성공", new TokenResponse(token)));
    }

    // DTO는 단순하므로 컨트롤러 내부에 선언
    @Getter
    @Setter
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    public record TokenResponse(String token) {}
}
