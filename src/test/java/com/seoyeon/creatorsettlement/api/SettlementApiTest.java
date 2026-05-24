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

/**
 * 정산 API 중 라이프사이클(생성/확정/지급) · 운영자 집계 · 스냅샷 read-through · 입력 검증을 검증한다.
 * (월별 계산 시나리오 자체는 SettlementScenarioTest 가 담당 → 중복 회피)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // 각 테스트 종료 시 롤백 → 시드 보존
class SettlementApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // ---------- 라이프사이클: PENDING → CONFIRMED → PAID ----------

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

        long id = om.readTree(created.getResponse().getContentAsString()).get("id").asLong();

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

    // ---------- 운영자 기간 집계 ----------

    @Test
    @DisplayName("운영자 집계 2025-03: creator-1 120,000 + creator-2 48,000, 전체 168,000")
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
    @DisplayName("운영자 집계 잘못된 기간(from > to)은 400")
    void admin_aggregate_invalidRange() throws Exception {
        mvc.perform(get("/api/admin/settlements")
                        .param("from", "2025-03-31")
                        .param("to", "2025-03-01"))
                .andExpect(status().isBadRequest());
    }

    // ---------- 스냅샷 read-through ----------

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

    @Test
    @DisplayName("미확정(PENDING) 정산은 스냅샷이 아니라 실시간 계산으로 응답")
    void getMonthly_pendingNotFrozen_recomputed() throws Exception {
        // PENDING 만 생성 (확정 안 함)
        mvc.perform(post("/api/creators/creator-1/settlements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("month", "2025-03"))))
                .andExpect(status().isCreated());

        // 새 환불 추가 → PENDING 은 실시간 계산이므로 반영되어야 함
        mvc.perform(post("/api/sales/sale-1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "id": "cancel-after-pending",
                              "refundAmount": 10000,
                              "canceledAt": "2025-03-10T10:00:00+09:00" }
                        """))
                .andExpect(status().isCreated());

        // 환불 110,000 + 10,000 = 120,000 반영 → payout 112,000 (재계산)
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRefundAmount").value(120000))
                .andExpect(jsonPath("$.payoutAmount").value(112000));
    }

    // ---------- 입력 검증 ----------

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
}
