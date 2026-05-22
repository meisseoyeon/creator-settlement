package com.seoyeon.creatorsettlement.service.settlement;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class SettlementCalculator {

    /**
     * 정산 계산 (순수 함수)
     *
     * @param sales   해당 기간에 결제된 판매 내역 (paidAt 기준 필터링되어 들어와야 함)
     * @param cancels 해당 기간에 취소된 환불 내역 (canceledAt 기준 필터링되어 들어와야 함)
     * @param rate    적용 수수료율 (0.20 = 20%)
     */
    public SettlementResult calculate(
            List<SaleRecord> sales,
            List<CancelRecord> cancels,
            BigDecimal rate
    ) {
        if (rate == null
                || rate.compareTo(BigDecimal.ZERO) < 0
                || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("수수료율은 0 이상 1 이하여야 합니다: " + rate);
        }

        long totalSales = sales.stream()
                .mapToLong(SaleRecord::getAmount)
                .sum();

        long totalRefund = cancels.stream()
                .mapToLong(CancelRecord::getRefundAmount)
                .sum();

        long netSales = totalSales - totalRefund;

        // 수수료 = max(0, netSales) * rate, 소수점 버림
        long commission = netSales <= 0
                ? 0L
                : BigDecimal.valueOf(netSales)
                .multiply(rate)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();

        long payout = netSales - commission;

        return new SettlementResult(
                totalSales,
                totalRefund,
                netSales,
                commission,
                payout,
                sales.size(),
                cancels.size(),
                rate
        );
    }
}