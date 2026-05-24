package com.seoyeon.creatorsettlement.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 헤더 기반 인증/인가 게이트 검증.
 * (기본 헤더 주입 없이 직접 X-User-Id / X-User-Role 을 지정해 401·403·허용을 확인)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationTest {

    @Autowired MockMvc mvc;

    private static final String UID = "X-User-Id";
    private static final String ROLE = "X-User-Role";

    @Test
    @DisplayName("인증 헤더가 없으면 401")
    void noHeader_unauthorized() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("알 수 없는 역할이면 401")
    void unknownRole_unauthorized() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03")
                        .header(UID, "creator-1").header(ROLE, "SUPERUSER"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("크리에이터 본인 정산 조회는 200")
    void creator_ownData_ok() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03")
                        .header(UID, "creator-1").header(ROLE, "CREATOR"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("크리에이터가 남의 정산 조회 시 403")
    void creator_othersData_forbidden() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03")
                        .header(UID, "creator-2").header(ROLE, "CREATOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 임의 크리에이터 정산 조회 가능 200")
    void admin_anyData_ok() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03")
                        .header(UID, "admin-1").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("크리에이터가 운영자 집계 호출 시 403")
    void creator_adminAggregate_forbidden() throws Exception {
        mvc.perform(get("/api/admin/settlements")
                        .param("from", "2025-03-01").param("to", "2025-03-31")
                        .header(UID, "creator-1").header(ROLE, "CREATOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("크리에이터가 정산 생성(POST) 시 403")
    void creator_createSettlement_forbidden() throws Exception {
        mvc.perform(post("/api/creators/creator-1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"month\":\"2025-03\"}")
                        .header(UID, "creator-1").header(ROLE, "CREATOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("크리에이터가 판매 등록(POST /api/sales) 시 403")
    void creator_registerSale_forbidden() throws Exception {
        mvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"sale-z\",\"courseId\":\"course-1\",\"studentId\":\"student-1\",\"amount\":1000,\"paidAt\":\"2025-04-01T10:00:00+09:00\"}")
                        .header(UID, "creator-1").header(ROLE, "CREATOR"))
                .andExpect(status().isForbidden());
    }
}
