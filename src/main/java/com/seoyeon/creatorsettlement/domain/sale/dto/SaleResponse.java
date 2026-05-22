package com.seoyeon.creatorsettlement.domain.sale.dto;

import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;

import java.time.OffsetDateTime;

public record SaleResponse(
        String id,
        String courseId,
        String courseTitle,
        String creatorId,
        String studentId,
        Long amount,
        OffsetDateTime paidAt,
        Long totalRefundedAmount,
        int cancelCount
) {
    public static SaleResponse from(SaleRecord s) {
        long refunded = s.getCancels().stream()
                .mapToLong(CancelRecord::getRefundAmount)
                .sum();
        return new SaleResponse(
                s.getId(),
                s.getCourse().getId(),
                s.getCourse().getTitle(),
                s.getCourse().getCreator().getId(),
                s.getStudentId(),
                s.getAmount(),
                s.getPaidAt(),
                refunded,
                s.getCancels().size()
        );
    }
}
