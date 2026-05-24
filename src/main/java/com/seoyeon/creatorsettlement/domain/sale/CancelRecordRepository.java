package com.seoyeon.creatorsettlement.domain.sale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {

    /** 특정 판매 건의 모든 취소 이력 (환불 누적 검증용) */
    List<CancelRecord> findBySale_Id(String saleId);

    /** 크리에이터별 + 취소일시 범위 취소 내역 (월별 정산용) */
    List<CancelRecord> findBySale_Course_Creator_IdAndCanceledAtGreaterThanEqualAndCanceledAtLessThan(
            String creatorId,
            OffsetDateTime from,
            OffsetDateTime toExclusive
    );

    /** 기간 내 전체 취소 (운영자 집계용). sale/course/creator 까지 fetch 해 N+1 방지. */
    @Query("select c from CancelRecord c " +
            "join fetch c.sale s join fetch s.course co join fetch co.creator " +
            "where c.canceledAt >= :from and c.canceledAt < :toExclusive")
    List<CancelRecord> findAllInPeriodWithCreator(
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive
    );
}
