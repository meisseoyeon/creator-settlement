package com.seoyeon.creatorsettlement.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // 각 테스트 종료 시 자동 롤백 → 시드 데이터 보존
class SettlementApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    @DisplayName("creator-1 2025-03 월별 정산: 총 260,000 / 환불 110,000 / 순 150,000 / 수수료 30,000 / 정산 120,000")
    void monthly_creator1_march() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatorId").value("creator-1"))
                .andExpect(jsonPath("$.totalSalesAmount").value(260000))
                .andExpect(jsonPath("$.totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.netSalesAmount").value(150000))
                .andExpect(jsonPath("$.commissionAmount").value(30000))
                .andExpect(jsonPath("$.payoutAmount").value(120000))
                .andExpect(jsonPath("$.salesCount").value(4))
                .andExpect(jsonPath("$.cancelCount").value(2));
    }

    @Test
    @DisplayName("월 경계 취소: creator-2 2025-02 는 1월 판매 제외, 2월 취소만 반영 → 순 -60,000")
    void monthly_creator2_feb_boundaryCancel() throws Exception {
        mvc.perform(get("/api/creators/creator-2/settlements").param("month", "2025-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(0))
                .andExpect(jsonPath("$.totalRefundAmount").value(60000))
                .andExpect(jsonPath("$.netSalesAmount").value(-60000))
                .andExpect(jsonPath("$.commissionAmount").value(0))
                .andExpect(jsonPath("$.payoutAmount").value(-60000))
                .andExpect(jsonPath("$.salesCount").value(0))
                .andExpect(jsonPath("$.cancelCount").value(1));
    }

    @Test
    @DisplayName("빈 월: creator-3 2025-03 은 판매/취소 없음 → 모든 금액 0")
    void monthly_creator3_march_empty() throws Exception {
        mvc.perform(get("/api/creators/creator-3/settlements").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(0))
                .andExpect(jsonPath("$.payoutAmount").value(0))
                .andExpect(jsonPath("$.salesCount").value(0))
                .andExpect(jsonPath("$.cancelCount").value(0));
    }

    @Test
    @DisplayName("잘못된 연월 형식은 400")
    void monthly_invalidMonth() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-13"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 크리에이터는 404")
    void monthly_creatorNotFound() throws Exception {
        mvc.perform(get("/api/creators/creator-x/settlements").param("month", "2025-03"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("운영자 집계: 2025-03 → creator-1 120,000 + creator-2 48,000, 전체 168,000")
    void admin_aggregate_march() throws Exception {
        mvc.perform(get("/api/admin/settlements")
                        .param("from", "2025-03-01")
                        .param("to", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creators.length()").value(2))
                .andExpect(jsonPath("$.creators[0].creatorId").value("creator-1"))
                .andExpect(jsonPath("$.creators[0].payoutAmount").value(120000))
                .andExpect(jsonPath("$.creators[1].creatorId").value("creator-2"))
                .andExpect(jsonPath("$.creators[1].payoutAmount").value(48000))
                .andExpect(jsonPath("$.total.creatorCount").value(2))
                .andExpect(jsonPath("$.total.payoutAmount").value(168000));
    }

    @Test
    @DisplayName("정산 라이프사이클: 생성(PENDING) → 확정(CONFIRMED) → 지급(PAID)")
    void lifecycle_pending_confirmed_paid() throws Exception {
        MvcResult created = mvc.perform(post("/api/creators/creator-1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("month", "2025-03"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payoutAmount").value(120000))
                .andReturn();

        JsonNode body = om.readTree(created.getResponse().getContentAsString());
        long id = body.get("id").asLong();

        mvc.perform(post("/api/settlements/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty());

        mvc.perform(post("/api/settlements/" + id + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }

    @Test
    @DisplayName("동일 (크리에이터, 월) 중복 정산 생성은 409")
    void duplicate_create_conflict() throws Exception {
        String reqBody = om.writeValueAsString(Map.of("month", "2025-03"));

        mvc.perform(post("/api/creators/creator-1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/creators/creator-1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PENDING 이 아닌 정산을 다시 확정하면 409")
    void confirm_invalidState_conflict() throws Exception {
        MvcResult created = mvc.perform(post("/api/creators/creator-1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("month", "2025-03"))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = om.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/settlements/" + id + "/confirm")).andExpect(status().isOk());
        // 이미 CONFIRMED → 다시 confirm 시 IllegalState → 409
        mvc.perform(post("/api/settlements/" + id + "/confirm")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("확정된 정산 조회는 스냅샷 반환 — 이후 환불이 추가돼도 금액 불변(재계산 안 함)")
    void getMonthly_returnsFrozenSnapshot_afterConfirm() throws Exception {
        // 1) 정산 생성(PENDING) → 확정(CONFIRMED)
        MvcResult created = mvc.perform(post("/api/creators/creator-1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("month", "2025-03"))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = om.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/settlements/" + id + "/confirm")).andExpect(status().isOk());

        // 2) 확정 이후 새 환불 추가 (실시간 계산이라면 payout 이 줄어들 데이터)
        mvc.perform(post("/api/sales/sale-1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "id": "cancel-after-confirm",
                              "refundAmount": 10000,
                              "canceledAt": "2025-03-10T10:00:00+09:00" }
                        """))
                .andExpect(status().isCreated());

        // 3) 조회 → 확정 스냅샷 그대로 (refund 110,000 / payout 120,000), 재계산 안 함
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.payoutAmount").value(120000));
    }
}
