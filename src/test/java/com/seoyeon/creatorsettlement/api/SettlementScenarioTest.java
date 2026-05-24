package com.seoyeon.creatorsettlement.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 과제 명세의 "샘플 데이터로 검증해야 할 시나리오" 4종을 시드 데이터 기준으로 1:1 검증.
 * (data.sql 의 creator-1~3 / sale-1~7 / cancel-1~3 기준)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // 각 테스트 종료 시 롤백 → 시드 보존
class SettlementScenarioTest {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("[시나리오1] creator-1 2025-03 정산: 판매 260,000 / 환불 110,000 / 순 150,000 / 수수료 30,000 / 정산 120,000")
    void scenario1_creator1_march() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(260000))
                .andExpect(jsonPath("$.totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.netSalesAmount").value(150000))
                .andExpect(jsonPath("$.commissionAmount").value(30000))
                .andExpect(jsonPath("$.payoutAmount").value(120000))
                .andExpect(jsonPath("$.salesCount").value(4))
                .andExpect(jsonPath("$.cancelCount").value(2));
    }

    @Test
    @DisplayName("[시나리오2] 부분 환불: sale-4(80,000)에 cancel-2(30,000) → 환불액 < 원결제, 부분 반영")
    void scenario2_partialRefund() throws Exception {
        // 판매 목록(2025-03)은 paidAt 오름차순: [sale-1, sale-2, sale-3, sale-4]
        mvc.perform(get("/api/creators/creator-1/sales")
                        .param("from", "2025-03-01")
                        .param("to", "2025-03-31"))
                .andExpect(status().isOk())
                // sale-4: 원결제 80,000 중 30,000 만 환불(부분), 취소 1건
                .andExpect(jsonPath("$[3].id").value("sale-4"))
                .andExpect(jsonPath("$[3].amount").value(80000))
                .andExpect(jsonPath("$[3].totalRefundedAmount").value(30000))
                .andExpect(jsonPath("$[3].cancelCount").value(1))
                // 비교군 sale-3: 전액 환불(80,000)
                .andExpect(jsonPath("$[2].id").value("sale-3"))
                .andExpect(jsonPath("$[2].totalRefundedAmount").value(80000));
    }

    @Test
    @DisplayName("[시나리오3-1월] 월 경계: sale-5(1/31 결제)는 creator-2 2025-01 판매에 반영 (취소는 아직 없음)")
    void scenario3_january_paymentSide() throws Exception {
        mvc.perform(get("/api/creators/creator-2/settlements").param("month", "2025-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(60000))
                .andExpect(jsonPath("$.totalRefundAmount").value(0))
                .andExpect(jsonPath("$.netSalesAmount").value(60000))
                .andExpect(jsonPath("$.commissionAmount").value(12000))
                .andExpect(jsonPath("$.payoutAmount").value(48000))
                .andExpect(jsonPath("$.salesCount").value(1))
                .andExpect(jsonPath("$.cancelCount").value(0));
    }

    @Test
    @DisplayName("[시나리오3-2월] 월 경계: cancel-3(2/2 취소)는 creator-2 2025-02 환불에 반영 → 순 판매 -60,000")
    void scenario3_february_cancelSide() throws Exception {
        mvc.perform(get("/api/creators/creator-2/settlements").param("month", "2025-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(0))
                .andExpect(jsonPath("$.totalRefundAmount").value(60000))
                .andExpect(jsonPath("$.netSalesAmount").value(-60000))
                .andExpect(jsonPath("$.commissionAmount").value(0))   // 순 판매 음수 → 수수료 0
                .andExpect(jsonPath("$.payoutAmount").value(-60000))
                .andExpect(jsonPath("$.salesCount").value(0))
                .andExpect(jsonPath("$.cancelCount").value(1));
    }

    @Test
    @DisplayName("[시나리오4] 빈 월: creator-3 2025-03 은 판매/취소 없음 → 모든 금액 0")
    void scenario4_emptyMonth() throws Exception {
        mvc.perform(get("/api/creators/creator-3/settlements").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(0))
                .andExpect(jsonPath("$.totalRefundAmount").value(0))
                .andExpect(jsonPath("$.netSalesAmount").value(0))
                .andExpect(jsonPath("$.commissionAmount").value(0))
                .andExpect(jsonPath("$.payoutAmount").value(0))
                .andExpect(jsonPath("$.salesCount").value(0))
                .andExpect(jsonPath("$.cancelCount").value(0));
    }
}
