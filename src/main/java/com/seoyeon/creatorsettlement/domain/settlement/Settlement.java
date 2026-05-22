package com.seoyeon.creatorsettlement.domain.settlement;
import com.seoyeon.creatorsettlement.domain.creator.Creator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Entity
@Table(name = "settlement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_creator_month",
                columnNames = {"creator_id", "settlement_month"}),
        indexes = @Index(name = "idx_settlement_status", columnList = "status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(name = "settlement_month", nullable = false, length = 7)
    private String settlementMonth;  // "2025-03"

    @Column(name = "total_sales_amount", nullable = false)
    private Long totalSalesAmount;

    @Column(name = "total_refund_amount", nullable = false)
    private Long totalRefundAmount;

    @Column(name = "net_sales_amount", nullable = false)
    private Long netSalesAmount;

    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount;

    @Column(name = "payout_amount", nullable = false)
    private Long payoutAmount;

    @Column(name = "sales_count", nullable = false)
    private Integer salesCount;

    @Column(name = "cancel_count", nullable = false)
    private Integer cancelCount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = SettlementStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /** PENDING → CONFIRMED 전이 */
    public void confirm() {
        if (status != SettlementStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태에서만 확정 가능합니다. 현재 상태: " + status);
        }
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = OffsetDateTime.now();
    }

    /** CONFIRMED → PAID 전이 */
    public void pay() {
        if (status != SettlementStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "CONFIRMED 상태에서만 지급 가능합니다. 현재 상태: " + status);
        }
        this.status = SettlementStatus.PAID;
        this.paidAt = OffsetDateTime.now();
    }
}
