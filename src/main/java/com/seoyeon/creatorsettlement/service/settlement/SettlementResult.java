package com.seoyeon.creatorsettlement.service.settlement;

import java.math.BigDecimal;

public record SettlementResult(
        long totalSalesAmount,
        long totalRefundAmount,
        long netSalesAmount,
        long commissionAmount,
        long payoutAmount,
        int salesCount,
        int cancelCount,
        BigDecimal commissionRate
) {
    public static SettlementResult empty(BigDecimal commissionRate) {
        return new SettlementResult(0, 0, 0, 0, 0, 0, 0, commissionRate);
    }
}
