package com.seoyeon.creatorsettlement.domain.sale;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, String> {

    /**
     * 환불 등록 시 판매 행에 쓰기 잠금(SELECT ... FOR UPDATE)을 건다.
     * 같은 판매에 대한 동시 환불을 직렬화해 "환불 누적 ≤ 원결제" 불변식을 보호한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaleRecord s where s.id = :id")
    Optional<SaleRecord> findByIdForUpdate(@Param("id") String id);

    /** 크리에이터별 + 결제일시 범위 판매 내역 조회 (paidAt 오름차순) */
    List<SaleRecord> findByCourse_Creator_IdAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtAsc(
            String creatorId,
            OffsetDateTime from,
            OffsetDateTime toExclusive
    );

    /** 크리에이터별 전체 (기간 필터 없을 때) */
    List<SaleRecord> findByCourse_Creator_IdOrderByPaidAtAsc(String creatorId);

    /** 기간 내 전체 판매 (운영자 집계용). course/creator 까지 fetch 해 N+1 방지. */
    @Query("select s from SaleRecord s " +
            "join fetch s.course c join fetch c.creator " +
            "where s.paidAt >= :from and s.paidAt < :toExclusive")
    List<SaleRecord> findAllInPeriodWithCreator(
            @Param("from") OffsetDateTime from,
            @Param("toExclusive") OffsetDateTime toExclusive
    );
}
