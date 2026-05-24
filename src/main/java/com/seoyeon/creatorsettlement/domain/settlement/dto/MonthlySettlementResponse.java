package com.seoyeon.creatorsettlement.domain.settlement.dto;

import com.seoyeon.creatorsettlement.domain.settlement.Settlement;
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

    /** 확정된 정산 스냅샷을 그대로 응답으로 변환 (재계산 없음). */
    public static MonthlySettlementResponse from(Settlement s) {
        return new MonthlySettlementResponse(
                s.getCreator().getId(), s.getCreator().getName(), s.getSettlementMonth(),
                s.getTotalSalesAmount(), s.getTotalRefundAmount(), s.getNetSalesAmount(),
                s.getCommissionAmount(), s.getPayoutAmount(),
                s.getSalesCount(), s.getCancelCount(), s.getCommissionRate());
    }
}
