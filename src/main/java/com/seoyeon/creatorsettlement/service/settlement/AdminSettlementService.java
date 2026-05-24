package com.seoyeon.creatorsettlement.service.settlement;

import com.seoyeon.creatorsettlement.common.DateRangeParser;
import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecordRepository;
import com.seoyeon.creatorsettlement.domain.settlement.dto.AdminSettlementResponse;
import com.seoyeon.creatorsettlement.domain.settlement.dto.AdminSettlementResponse.CreatorSettlementSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 운영자용 기간별 전체 정산 집계를 담당한다.
 * 활동(판매 또는 취소)이 있는 크리에이터만 집계 대상에 포함한다.
 */
@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final SaleRecordRepository saleRepo;
    private final CancelRecordRepository cancelRepo;
    private final SettlementCalculator calculator;
    private final CommissionPolicy commissionPolicy;

    @Transactional(readOnly = true)
    public AdminSettlementResponse aggregate(String from, String to) {
        OffsetDateTime start = DateRangeParser.parseStartOfDay(from);
        OffsetDateTime end = DateRangeParser.parseEndOfDayExclusive(to);
        if (!start.isBefore(end)) {
            throw BusinessException.badRequest("from 은 to 보다 이전이거나 같아야 합니다.");
        }

        List<SaleRecord> sales = saleRepo.findAllInPeriodWithCreator(start, end);
        List<CancelRecord> cancels = cancelRepo.findAllInPeriodWithCreator(start, end);

        Map<String, List<SaleRecord>> salesByCreator = sales.stream()
                .collect(Collectors.groupingBy(s -> s.getCourse().getCreator().getId()));
        Map<String, List<CancelRecord>> cancelsByCreator = cancels.stream()
                .collect(Collectors.groupingBy(c -> c.getSale().getCourse().getCreator().getId()));

        Map<String, String> creatorNames = new HashMap<>();
        sales.forEach(s -> creatorNames.putIfAbsent(
                s.getCourse().getCreator().getId(), s.getCourse().getCreator().getName()));
        cancels.forEach(c -> creatorNames.putIfAbsent(
                c.getSale().getCourse().getCreator().getId(), c.getSale().getCourse().getCreator().getName()));

        BigDecimal rate = commissionPolicy.currentRate();
        List<CreatorSettlementSummary> summaries = creatorNames.keySet().stream()
                .sorted()
                .map(creatorId -> CreatorSettlementSummary.of(
                        creatorId,
                        creatorNames.get(creatorId),
                        calculator.calculate(
                                salesByCreator.getOrDefault(creatorId, List.of()),
                                cancelsByCreator.getOrDefault(creatorId, List.of()),
                                rate)))
                .toList();

        return AdminSettlementResponse.of(from, to, summaries);
    }
}
