package com.seoyeon.creatorsettlement.domain.settlement.dto;

import com.seoyeon.creatorsettlement.domain.settlement.SettlementResult;

import java.math.BigDecimal;

public record MonthlySettlementResponse(
        String creatorId,
        String creatorName,
        String month,
        long totalSalesAmount,
        long totalRefundAmount,
        long netSalesAmount,
        long commissionAmount,
        long payoutAmount,
        int salesCount,
        int cancelCount,
        BigDecimal commissionRate
) {
    public static MonthlySettlementResponse of(
            String creatorId, String creatorName, String month, SettlementResult r) {
        return new MonthlySettlementResponse(
                creatorId, creatorName, month,
                r.totalSalesAmount(), r.totalRefundAmount(), r.netSalesAmount(),
                r.commissionAmount(), r.payoutAmount(),
                r.salesCount(), r.cancelCount(), r.commissionRate());
    }
}
