package com.seoyeon.creatorsettlement.service.settlement;

import com.seoyeon.creatorsettlement.common.MonthRange;
import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import com.seoyeon.creatorsettlement.domain.creator.Creator;
import com.seoyeon.creatorsettlement.domain.creator.CreatorRepository;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecordRepository;
import com.seoyeon.creatorsettlement.domain.settlement.Settlement;
import com.seoyeon.creatorsettlement.domain.settlement.SettlementRepository;
import com.seoyeon.creatorsettlement.domain.settlement.SettlementResult;
import com.seoyeon.creatorsettlement.domain.settlement.SettlementStatus;
import com.seoyeon.creatorsettlement.domain.settlement.dto.MonthlySettlementResponse;
import com.seoyeon.creatorsettlement.domain.settlement.dto.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 크리에이터 월별 정산을 담당한다.
 * - 조회: 확정(CONFIRMED)·지급(PAID)된 달은 저장된 스냅샷을 반환(재계산 X),
 *         미확정 달만 결제/취소 내역으로부터 즉시 계산
 * - 확정: 계산 결과를 스냅샷으로 저장하고 PENDING → CONFIRMED → PAID 로 전이
 *
 * 정산 기간 기준: 결제는 paidAt, 취소는 canceledAt 기준 (KST 월 경계).
 */
@Service
@RequiredArgsConstructor
public class CreatorSettlementService {

    private final CreatorRepository creatorRepo;
    private final SaleRecordRepository saleRepo;
    private final CancelRecordRepository cancelRepo;
    private final SettlementRepository settlementRepo;
    private final SettlementCalculator calculator;
    private final CommissionPolicy commissionPolicy;

    // 월별 정산 조회
    @Transactional(readOnly = true)
    public MonthlySettlementResponse getMonthly(String creatorId, String month) {
        Creator creator = requireCreator(creatorId);

        // 확정(CONFIRMED)·지급(PAID)된 정산은 불변이므로 재계산 없이 저장된 스냅샷을 반환
        Optional<Settlement> finalized = settlementRepo
                .findByCreator_IdAndSettlementMonth(creatorId, month)
                .filter(s -> s.getStatus() != SettlementStatus.PENDING);
        if (finalized.isPresent()) {
            return MonthlySettlementResponse.from(finalized.get());
        }

        SettlementResult result = compute(creatorId, MonthRange.of(month));
        return MonthlySettlementResponse.of(creator.getId(), creator.getName(), month, result);
    }

    // 월별 정산 스냅샷 생성 (PENDING). 동일 (크리에이터, 월) 중복 생성은 409
    @Transactional
    public SettlementResponse create(String creatorId, String month) {
        Creator creator = requireCreator(creatorId);
        MonthRange range = MonthRange.of(month);
        if (settlementRepo.existsByCreator_IdAndSettlementMonth(creatorId, month)) {
            throw BusinessException.conflict(
                    "이미 정산이 생성된 월입니다: " + creatorId + " / " + month);
        }
        SettlementResult result = compute(creatorId, range);
        Settlement saved = settlementRepo.save(Settlement.of(creator, month, result));
        return SettlementResponse.from(saved);
    }
    // 상태 변경
    @Transactional
    public SettlementResponse confirm(Long settlementId) {
        Settlement settlement = requireSettlement(settlementId);
        settlement.confirm();
        return SettlementResponse.from(settlement);
    }

    @Transactional
    public SettlementResponse pay(Long settlementId) {
        Settlement settlement = requireSettlement(settlementId);
        settlement.pay();
        return SettlementResponse.from(settlement);
    }
    // 단건 조회
    @Transactional(readOnly = true)
    public SettlementResponse get(Long settlementId) {
        return SettlementResponse.from(requireSettlement(settlementId));
    }

    private SettlementResult compute(String creatorId, MonthRange range) {
        List<SaleRecord> sales = saleRepo
                .findByCourse_Creator_IdAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtAsc(
                        creatorId, range.startInclusive(), range.endExclusive());
        List<CancelRecord> cancels = cancelRepo
                .findBySale_Course_Creator_IdAndCanceledAtGreaterThanEqualAndCanceledAtLessThan(
                        creatorId, range.startInclusive(), range.endExclusive());
        return calculator.calculate(sales, cancels, commissionPolicy.currentRate());
    }

    private Creator requireCreator(String creatorId) {
        return creatorRepo.findById(creatorId)
                .orElseThrow(() -> BusinessException.notFound(
                        "존재하지 않는 크리에이터입니다: " + creatorId));
    }

    private Settlement requireSettlement(Long settlementId) {
        return settlementRepo.findById(settlementId)
                .orElseThrow(() -> BusinessException.notFound(
                        "존재하지 않는 정산입니다: " + settlementId));
    }
}
