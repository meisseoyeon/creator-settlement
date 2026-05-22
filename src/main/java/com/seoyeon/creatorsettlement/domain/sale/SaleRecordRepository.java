package com.seoyeon.creatorsettlement.domain.sale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, String> {

    /** 크리에이터별 + 결제일시 범위 판매 내역 조회 (paidAt 오름차순) */
    List<SaleRecord> findByCourse_Creator_IdAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtAsc(
            String creatorId,
            OffsetDateTime from,
            OffsetDateTime toExclusive
    );

    /** 크리에이터별 전체 (기간 필터 없을 때) */
    List<SaleRecord> findByCourse_Creator_IdOrderByPaidAtAsc(String creatorId);
}
