package com.park302.dashboard.common.enums;

/**
 * 노출 여부 공통 Enum
 * DB 컬럼 타입: CHAR(1), @Enumerated(EnumType.STRING)으로 매핑
 * Y = 노출(활성), N = 숨김(soft delete 포함)
 */
public enum IsVisible {
    Y, N
}
