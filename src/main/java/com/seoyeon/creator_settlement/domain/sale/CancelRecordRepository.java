package com.seoyeon.creator_settlement.domain.sale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {

    /** 특정 판매 건의 모든 취소 이력 (환불 누적 검증용) */
    List<CancelRecord> findBySale_Id(String saleId);
}