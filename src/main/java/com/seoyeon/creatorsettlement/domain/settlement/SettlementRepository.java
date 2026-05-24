package com.seoyeon.creatorsettlement.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    boolean existsByCreator_IdAndSettlementMonth(String creatorId, String settlementMonth);

    Optional<Settlement> findByCreator_IdAndSettlementMonth(String creatorId, String settlementMonth);
}
