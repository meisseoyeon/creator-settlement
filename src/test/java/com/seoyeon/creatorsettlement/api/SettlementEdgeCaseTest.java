package com.seoyeon.creatorsettlement.api;

import com.seoyeon.creatorsettlement.support.MockMvcAuthConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 과제 기본 시나리오 외에 직접 추가한 보강 검증.
 * - 동일 판매 다회 부분 환불 + 누적 경계
 * - 미래 월 조회 일관성
 * - 운영자 집계의 음수/양수 정산 합산
 * (왜 추가했는지는 README "추가 검증 시나리오" 참고)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)   // 기본 관리자 인증 헤더 주입
@Transactional
class SettlementEdgeCaseTest {

    @Autowired MockMvc mvc;

    private void registerCancel(String saleId, String id, long amount, String canceledAt) throws Exception {
        mvc.perform(post("/api/sales/" + saleId + "/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "id": "%s", "refundAmount": %d, "canceledAt": "%s" }
                            """.formatted(id, amount, canceledAt)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("동일 판매 다회 부분 환불: 누적이 원결제와 같으면 허용, 1원이라도 초과하면 400")
    void multiplePartialRefunds_cumulativeBoundary() throws Exception {
        // sale-1: 원결제 50,000, 기존 취소 없음
        registerCancel("sale-1", "extra-cancel-1", 20000, "2025-03-06T10:00:00+09:00");
        registerCancel("sale-1", "extra-cancel-2", 30000, "2025-03-07T10:00:00+09:00"); // 누적 50,000 == 원결제 → 허용

        // 1원만 더해도 누적 초과 → 400
        mvc.perform(post("/api/sales/sale-1/cancels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "id": "extra-cancel-3", "refundAmount": 1, "canceledAt": "2025-03-08T10:00:00+09:00" }
                            """))
                .andExpect(status().isBadRequest());

        // 정산 반영: creator-1 2025-03 환불 = 110,000(기존) + 50,000(신규) = 160,000
        //   판매 260,000 → 순 100,000 / 수수료 20,000 / 정산 80,000 / 취소 4건
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRefundAmount").value(160000))
                .andExpect(jsonPath("$.netSalesAmount").value(100000))
                .andExpect(jsonPath("$.commissionAmount").value(20000))
                .andExpect(jsonPath("$.payoutAmount").value(80000))
                .andExpect(jsonPath("$.cancelCount").value(4));
    }

    @Test
    @DisplayName("미래 월 조회: 데이터 없는 먼 미래 월도 빈 월과 동일하게 0원 응답")
    void futureMonth_returnsZero() throws Exception {
        mvc.perform(get("/api/creators/creator-1/settlements").param("month", "2099-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(0))
                .andExpect(jsonPath("$.totalRefundAmount").value(0))
                .andExpect(jsonPath("$.payoutAmount").value(0))
                .andExpect(jsonPath("$.salesCount").value(0))
                .andExpect(jsonPath("$.cancelCount").value(0));
    }

    @Test
    @DisplayName("[추가시드] creator-4 2025-04: 한 판매 다회 환불 + 수수료 버림 → 순 83,333 / 수수료 16,666(버림) / 정산 66,667")
    void seed_creator4_april_multiCancelAndRounding() throws Exception {
        // sale-8(100,000)+sale-9(33,333)=133,333, cancel-4+5=50,000 → net 83,333
        // 83,333 * 0.2 = 16,666.6 → DOWN → 16,666, payout 66,667
        mvc.perform(get("/api/creators/creator-4/settlements").param("month", "2025-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(133333))
                .andExpect(jsonPath("$.totalRefundAmount").value(50000))
                .andExpect(jsonPath("$.netSalesAmount").value(83333))
                .andExpect(jsonPath("$.commissionAmount").value(16666))   // 소수점 버림
                .andExpect(jsonPath("$.payoutAmount").value(66667))
                .andExpect(jsonPath("$.salesCount").value(2))
                .andExpect(jsonPath("$.cancelCount").value(2));
    }

    @Test
    @DisplayName("[추가시드] 동일 판매(sale-8) 다회 부분 환불: 누적 50,000 / 취소 2건이 판매 내역에 반영")
    void seed_sale8_multipleCancels() throws Exception {
        // 2025-04 판매는 paidAt 오름차순: [sale-8(04-05), sale-9(04-20)]
        mvc.perform(get("/api/creators/creator-4/sales")
                        .param("from", "2025-04-01")
                        .param("to", "2025-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("sale-8"))
                .andExpect(jsonPath("$[0].amount").value(100000))
                .andExpect(jsonPath("$[0].totalRefundedAmount").value(50000))
                .andExpect(jsonPath("$[0].cancelCount").value(2));
    }

    @Test
    @DisplayName("[추가시드] creator-4 2025-05: 같은 달 결제+전액환불 → 순 판매 0 / 정산 0 (빈 월과 구분)")
    void seed_creator4_may_netZero() throws Exception {
        mvc.perform(get("/api/creators/creator-4/settlements").param("month", "2025-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSalesAmount").value(40000))   // 판매·취소는 존재
                .andExpect(jsonPath("$.totalRefundAmount").value(40000))
                .andExpect(jsonPath("$.netSalesAmount").value(0))
                .andExpect(jsonPath("$.commissionAmount").value(0))
                .andExpect(jsonPath("$.payoutAmount").value(0))
                .andExpect(jsonPath("$.salesCount").value(1))
                .andExpect(jsonPath("$.cancelCount").value(1));
    }

    @Test
    @DisplayName("운영자 집계 2025-02: 음수 정산(creator-2 -60,000)과 양수 정산(creator-3 +96,000)이 합산되어 36,000")
    void adminAggregate_february_mixesNegativeAndPositive() throws Exception {
        // creator-2: 2월 판매 0 / cancel-3 환불 60,000 → 순 -60,000 / payout -60,000
        // creator-3: 2월 sale-7 120,000 / 환불 0 → 순 120,000 / 수수료 24,000 / payout 96,000
        mvc.perform(get("/api/admin/settlements")
                        .param("from", "2025-02-01")
                        .param("to", "2025-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creators.length()").value(2))
                .andExpect(jsonPath("$.creators[0].creatorId").value("creator-2"))
                .andExpect(jsonPath("$.creators[0].payoutAmount").value(-60000))
                .andExpect(jsonPath("$.creators[1].creatorId").value("creator-3"))
                .andExpect(jsonPath("$.creators[1].payoutAmount").value(96000))
                .andExpect(jsonPath("$.total.creatorCount").value(2))
                .andExpect(jsonPath("$.total.payoutAmount").value(36000));
    }
}
