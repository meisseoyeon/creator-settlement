package com.seoyeon.creatorsettlement.service;

import com.seoyeon.creatorsettlement.domain.course.Course;
import com.seoyeon.creatorsettlement.domain.creator.Creator;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.service.settlement.SettlementCalculator;
import com.seoyeon.creatorsettlement.service.settlement.SettlementResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementCalculatorTest {

    private final SettlementCalculator calculator = new SettlementCalculator();
    private static final BigDecimal RATE_20 = new BigDecimal("0.20");

    // ---------- 테스트 픽스처 헬퍼 ----------

    private Creator creator(String id) {
        return new Creator(id, "테스트크리에이터");
    }

    private Course course(String id, Creator c) {
        return new Course(id, c, "테스트코스");
    }

    private SaleRecord sale(String id, Course c, long amount, String paidAt) {
        return new SaleRecord(id, c, "student-x", amount, OffsetDateTime.parse(paidAt));
    }

    private CancelRecord cancel(String id, SaleRecord s, long refund, String canceledAt) {
        return new CancelRecord(id, s, refund, OffsetDateTime.parse(canceledAt));
    }

    // ---------- 핵심 시나리오 ----------

    @Test
    @DisplayName("creator-1의 2025-03 정산: 총 260,000 / 환불 110,000 / 순 150,000 / 수수료 30,000 / 정산 120,000")
    void creator1_march_2025() {
        Creator c1 = creator("creator-1");
        Course course1 = course("course-1", c1);
        Course course2 = course("course-2", c1);

        SaleRecord s1 = sale("sale-1", course1, 50000, "2025-03-05T10:00:00+09:00");
        SaleRecord s2 = sale("sale-2", course1, 50000, "2025-03-15T14:30:00+09:00");
        SaleRecord s3 = sale("sale-3", course2, 80000, "2025-03-20T09:00:00+09:00");
        SaleRecord s4 = sale("sale-4", course2, 80000, "2025-03-22T11:00:00+09:00");

        CancelRecord cancel1 = cancel("cancel-1", s3, 80000, "2025-03-25T10:00:00+09:00");
        CancelRecord cancel2 = cancel("cancel-2", s4, 30000, "2025-03-28T15:00:00+09:00");

        SettlementResult result = calculator.calculate(
                List.of(s1, s2, s3, s4),
                List.of(cancel1, cancel2),
                RATE_20
        );

        assertThat(result.totalSalesAmount()).isEqualTo(260_000L);
        assertThat(result.totalRefundAmount()).isEqualTo(110_000L);
        assertThat(result.netSalesAmount()).isEqualTo(150_000L);
        assertThat(result.commissionAmount()).isEqualTo(30_000L);
        assertThat(result.payoutAmount()).isEqualTo(120_000L);
        assertThat(result.salesCount()).isEqualTo(4);
        assertThat(result.cancelCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("판매도 환불도 없는 빈 월: 모든 값 0")
    void empty_month() {
        SettlementResult result = calculator.calculate(List.of(), List.of(), RATE_20);

        assertThat(result.totalSalesAmount()).isZero();
        assertThat(result.totalRefundAmount()).isZero();
        assertThat(result.netSalesAmount()).isZero();
        assertThat(result.commissionAmount()).isZero();
        assertThat(result.payoutAmount()).isZero();
        assertThat(result.salesCount()).isZero();
        assertThat(result.cancelCount()).isZero();
    }

    @Test
    @DisplayName("부분 환불: 환불액이 원결제 금액보다 작은 경우 차액만큼 순 판매에 반영")
    void partial_refund() {
        Creator c = creator("creator-1");
        Course course = course("course-2", c);

        SaleRecord s = sale("sale-4", course, 80000, "2025-03-22T11:00:00+09:00");
        CancelRecord cancel = cancel("cancel-2", s, 30000, "2025-03-28T15:00:00+09:00");

        SettlementResult result = calculator.calculate(
                List.of(s), List.of(cancel), RATE_20);

        assertThat(result.totalSalesAmount()).isEqualTo(80_000L);
        assertThat(result.totalRefundAmount()).isEqualTo(30_000L);
        assertThat(result.netSalesAmount()).isEqualTo(50_000L);
        assertThat(result.commissionAmount()).isEqualTo(10_000L);
        assertThat(result.payoutAmount()).isEqualTo(40_000L);
    }

    @Test
    @DisplayName("환불만 있고 판매가 없는 달: 음수 순 판매, 수수료는 0, payout은 음수")
    void refund_only_month() {
        Creator c = creator("creator-2");
        Course course = course("course-3", c);

        // sale-5는 1월에 결제됐지만, 2월 정산을 계산할 때는 sales 리스트가 비어 있음
        SaleRecord s = sale("sale-5", course, 60000, "2025-01-31T23:30:00+09:00");
        CancelRecord cancel = cancel("cancel-3", s, 60000, "2025-02-02T09:00:00+09:00");

        SettlementResult result = calculator.calculate(
                List.of(), List.of(cancel), RATE_20);

        assertThat(result.totalSalesAmount()).isZero();
        assertThat(result.totalRefundAmount()).isEqualTo(60_000L);
        assertThat(result.netSalesAmount()).isEqualTo(-60_000L);
        assertThat(result.commissionAmount()).isZero();  // 정책: 음수면 수수료 0
        assertThat(result.payoutAmount()).isEqualTo(-60_000L);
        assertThat(result.salesCount()).isZero();
        assertThat(result.cancelCount()).isEqualTo(1);
    }

    @Nested
    @DisplayName("수수료율 경계값")
    class CommissionRateBoundary {

        @Test
        @DisplayName("수수료율 0%: 수수료 0, payout = netSales")
        void rate_zero() {
            Creator c = creator("creator-1");
            Course course = course("course-1", c);
            SaleRecord s = sale("sale-x", course, 100000, "2025-03-01T10:00:00+09:00");

            SettlementResult result = calculator.calculate(
                    List.of(s), List.of(), BigDecimal.ZERO);

            assertThat(result.commissionAmount()).isZero();
            assertThat(result.payoutAmount()).isEqualTo(100_000L);
        }

        @Test
        @DisplayName("수수료율 100%: 수수료 = netSales, payout = 0")
        void rate_one() {
            Creator c = creator("creator-1");
            Course course = course("course-1", c);
            SaleRecord s = sale("sale-x", course, 100000, "2025-03-01T10:00:00+09:00");

            SettlementResult result = calculator.calculate(
                    List.of(s), List.of(), BigDecimal.ONE);

            assertThat(result.commissionAmount()).isEqualTo(100_000L);
            assertThat(result.payoutAmount()).isZero();
        }

        @Test
        @DisplayName("음수 또는 1 초과 수수료율은 IllegalArgumentException")
        void invalid_rate() {
            assertThatThrownBy(() ->
                    calculator.calculate(List.of(), List.of(), new BigDecimal("-0.01"))
            ).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() ->
                    calculator.calculate(List.of(), List.of(), new BigDecimal("1.01"))
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("수수료 소수점은 RoundingMode.DOWN(버림)으로 처리")
        void rounding_down() {
            Creator c = creator("creator-1");
            Course course = course("course-1", c);
            // 100원 * 0.20 = 20원이지만, 99원 * 0.20 = 19.8원 → 19원으로 버림
            SaleRecord s = sale("sale-x", course, 99, "2025-03-01T10:00:00+09:00");

            SettlementResult result = calculator.calculate(
                    List.of(s), List.of(), RATE_20);

            assertThat(result.commissionAmount()).isEqualTo(19L);
            assertThat(result.payoutAmount()).isEqualTo(80L);
        }
    }
}