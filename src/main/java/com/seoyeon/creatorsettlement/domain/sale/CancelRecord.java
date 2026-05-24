package com.seoyeon.creatorsettlement.domain.sale;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
@Entity
@Table(name = "cancel_record",
        indexes = {
                @Index(name = "idx_cancel_canceled_at", columnList = "canceled_at"),
                // 판매별 환불 누적 검증 + 월별 취소 범위 조회 → 복합 인덱스
                @Index(name = "idx_cancel_sale_canceled_at", columnList = "sale_id, canceled_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancelRecord {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private SaleRecord sale;

    @Column(name = "refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "canceled_at", nullable = false)
    private OffsetDateTime canceledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public CancelRecord(String id, SaleRecord sale, Long refundAmount,
                        OffsetDateTime canceledAt) {
        this.id = id;
        this.sale = sale;
        this.refundAmount = refundAmount;
        this.canceledAt = canceledAt;
    }
}
