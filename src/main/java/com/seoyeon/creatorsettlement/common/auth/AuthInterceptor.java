package com.seoyeon.creatorsettlement.common.auth;

import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 헤더 기반 간략 인증/인가.
 * - 인증: X-User-Id, X-User-Role (CREATOR/ADMIN) 헤더로 호출자 식별. 없으면 401.
 * - 인가:
 *   · 운영 영역(/api/admin, /api/settlements, /api/sales, 정산 생성 POST) → ADMIN 만
 *   · 크리에이터 조회(GET /api/creators/{creatorId}/**) → 본인(id 일치) 또는 ADMIN
 */
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLE_HEADER = "X-User-Role";

    private static final String CREATORS_PREFIX = "/api/creators/";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(USER_ID_HEADER);
        String roleRaw = request.getHeader(ROLE_HEADER);

        if (isBlank(userId) || isBlank(roleRaw)) {
            throw BusinessException.unauthorized(
                    "인증 정보가 없습니다. " + USER_ID_HEADER + " / " + ROLE_HEADER + " 헤더가 필요합니다.");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BusinessException.unauthorized("알 수 없는 역할입니다: " + roleRaw);
        }

        boolean isAdmin = role == UserRole.ADMIN;
        String path = request.getRequestURI();

        // 운영(관리자) 전용 영역: 집계 / 정산 상태관리 / 판매·취소 등록
        if (path.startsWith("/api/admin/")
                || path.startsWith("/api/settlements/")
                || path.startsWith("/api/sales")) {
            requireAdmin(isAdmin, "해당 작업은 관리자 권한이 필요합니다.");
            return true;
        }

        // 크리에이터 스코프
        if (path.startsWith(CREATORS_PREFIX)) {
            // 정산 생성 등 변경 작업(POST 등)은 운영자 업무 → ADMIN
            if (!"GET".equalsIgnoreCase(request.getMethod())) {
                requireAdmin(isAdmin, "해당 작업은 관리자 권한이 필요합니다.");
                return true;
            }
            // 조회는 본인 또는 ADMIN
            String creatorId = extractCreatorId(path);
            if (isAdmin || userId.equals(creatorId)) {
                return true;
            }
            throw BusinessException.forbidden("본인(" + creatorId + ")의 데이터만 조회할 수 있습니다.");
        }

        return true;
    }

    private void requireAdmin(boolean isAdmin, String message) {
        if (!isAdmin) {
            throw BusinessException.forbidden(message);
        }
    }

    /** "/api/creators/{creatorId}/..." 에서 creatorId 추출 */
    private String extractCreatorId(String path) {
        String rest = path.substring(CREATORS_PREFIX.length());
        int slash = rest.indexOf('/');
        return slash >= 0 ? rest.substring(0, slash) : rest;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
