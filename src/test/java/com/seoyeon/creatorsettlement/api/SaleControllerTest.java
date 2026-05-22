package com.seoyeon.creatorsettlement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // 각 테스트 종료 시 자동 롤백 → 시드 데이터 보존
class SaleControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    @DisplayName("판매 등록 → 201 Created + Location 헤더")
    void registerSale_ok() throws Exception {
        Map<String, Object> body = Map.of(
                "id", "sale-test-1",
                "courseId", "course-1",
                "studentId", "student-1",
                "amount", 50000,
                "paidAt", "2025-04-01T10:00:00+09:00"
        );
        mvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/sales/sale-test-1"))
                .andExpect(jsonPath("$.id").value("sale-test-1"))
                .andExpect(jsonPath("$.amount").value(50000));
    }

    @Test
    @DisplayName("존재하지 않는 강의로 등록 시 404")
    void registerSale_courseNotFound() throws Exception {
        Map<String, Object> body = Map.of(
                "id", "sale-test-x",
                "courseId", "course-not-exist",
                "studentId", "student-1",
                "amount", 50000,
                "paidAt", "2025-04-01T10:00:00+09:00"
        );
        mvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("환불 누적이 원결제 초과 시 400")
    void registerCancel_exceedsAmount() throws Exception {
        mvc.perform(post("/api/sales/sale-1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    { "id": "cancel-overflow",
                      "refundAmount": 60000,
                      "canceledAt": "2025-03-10T10:00:00+09:00" }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("크리에이터 판매 조회 - 기간 필터로 3월 데이터 4건")
    void listByCreator_marchFilter() throws Exception {
        mvc.perform(get("/api/creators/creator-1/sales")
                        .param("from", "2025-03-01")
                        .param("to", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("취소 일시가 결제 일시보다 빠르면 400")
    void registerCancel_invalidTime() throws Exception {
        mvc.perform(post("/api/sales/sale-1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    { "id": "cancel-time-error",
                      "refundAmount": 10000,
                      "canceledAt": "2025-03-01T10:00:00+09:00" }
                """))
                .andExpect(status().isBadRequest());
    }
}
