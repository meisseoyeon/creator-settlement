package com.seoyeon.creator_settlement.domain.sale.dto;

import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;

import java.time.OffsetDateTime;

public record CancelResponse(
        String id,
        String saleId,
        Long refundAmount,
        OffsetDateTime canceledAt
) {
    public static CancelResponse from(CancelRecord c) {
        return new CancelResponse(
                c.getId(),
                c.getSale().getId(),
                c.getRefundAmount(),
                c.getCanceledAt()
        );
    }
}