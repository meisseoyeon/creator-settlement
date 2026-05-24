package com.seoyeon.creatorsettlement.domain.settlement.dto;

import com.seoyeon.creatorsettlement.domain.settlement.Settlement;
import com.seoyeon.creatorsettlement.domain.settlement.SettlementStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SettlementResponse(
        Long id,
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
        BigDecimal commissionRate,
        SettlementStatus status,
        OffsetDateTime confirmedAt,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static SettlementResponse from(Settlement s) {
        return new SettlementResponse(
                s.getId(),
                s.getCreator().getId(),
                s.getCreator().getName(),
                s.getSettlementMonth(),
                s.getTotalSalesAmount(),
                s.getTotalRefundAmount(),
                s.getNetSalesAmount(),
                s.getCommissionAmount(),
                s.getPayoutAmount(),
                s.getSalesCount(),
                s.getCancelCount(),
                s.getCommissionRate(),
                s.getStatus(),
                s.getConfirmedAt(),
                s.getPaidAt(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
