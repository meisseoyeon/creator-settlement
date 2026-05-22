package com.seoyeon.creatorsettlement.service.sale;

import com.seoyeon.creatorsettlement.common.DateRangeParser;
import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import com.seoyeon.creatorsettlement.domain.creator.CreatorRepository;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/** 판매 내역 조회만 담당한다. */
@Service
@RequiredArgsConstructor
public class SaleQueryService {

    private final SaleRecordRepository saleRepo;
    private final CreatorRepository creatorRepo;

    @Transactional(readOnly = true)
    public List<SaleRecord> findByCreator(String creatorId, String from, String to) {
        if (!creatorRepo.existsById(creatorId)) {
            throw BusinessException.notFound("존재하지 않는 크리에이터입니다: " + creatorId);
        }

        if (from == null && to == null) {
            return saleRepo.findByCourse_Creator_IdOrderByPaidAtAsc(creatorId);
        }
        if (from == null || to == null) {
            throw BusinessException.badRequest("from, to 는 함께 지정해야 합니다.");
        }

        OffsetDateTime fromDt = DateRangeParser.parseStartOfDay(from);
        OffsetDateTime toExclusive = DateRangeParser.parseEndOfDayExclusive(to);

        if (!fromDt.isBefore(toExclusive)) {
            throw BusinessException.badRequest("from 은 to 보다 이전이어야 합니다.");
        }

        return saleRepo
                .findByCourse_Creator_IdAndPaidAtGreaterThanEqualAndPaidAtLessThanOrderByPaidAtAsc(
                        creatorId, fromDt, toExclusive);
    }
}
