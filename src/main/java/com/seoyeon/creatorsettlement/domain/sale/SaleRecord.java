package com.seoyeon.creatorsettlement.domain.sale;
import com.seoyeon.creatorsettlement.domain.course.Course;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_record",
        indexes = {
                @Index(name = "idx_sale_paid_at", columnList = "paid_at"),
                // 크리에이터 월별 조회: course 로 join 후 paid_at 범위 스캔 → 복합 인덱스
                @Index(name = "idx_sale_course_paid_at", columnList = "course_id, paid_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleRecord {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "student_id", nullable = false, length = 50)
    private String studentId;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "sale", fetch = FetchType.LAZY)
    private List<CancelRecord> cancels = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public SaleRecord(String id, Course course, String studentId,
                      Long amount, OffsetDateTime paidAt) {
        this.id = id;
        this.course = course;
        this.studentId = studentId;
        this.amount = amount;
        this.paidAt = paidAt;
    }
}
