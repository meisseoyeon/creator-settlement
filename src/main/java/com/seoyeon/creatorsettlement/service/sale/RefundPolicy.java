package com.seoyeon.creatorsettlement.service.sale;

import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 환불(취소) 가능 여부를 검증한다.
 * 환불 정책이 바뀌면 이 클래스만 수정한다.
 */
@Component
@RequiredArgsConstructor
public class RefundPolicy {

    private final CancelRecordRepository cancelRepo;

    /**
     * 취소 일시는 결제 일시 이후여야 한다.
     */
    public void validateTime(SaleRecord sale, OffsetDateTime canceledAt) {
        if (canceledAt.isBefore(sale.getPaidAt())) {
            throw BusinessException.badRequest("취소 일시는 결제 일시 이후여야 합니다.");
        }
    }

    /**
     * 기존 환불 누적 + 신규 환불액이 원 결제 금액을 초과할 수 없다.
     */
    public void validateRefundAmount(SaleRecord sale, long newRefundAmount) {
        long alreadyRefunded = cancelRepo.findBySale_Id(sale.getId()).stream()
                .mapToLong(CancelRecord::getRefundAmount)
                .sum();
        long afterRefund = alreadyRefunded + newRefundAmount;

        if (afterRefund > sale.getAmount()) {
            throw BusinessException.badRequest(String.format(
                    "환불 누적액(%d원)이 원결제 금액(%d원)을 초과합니다.",
                    afterRefund, sale.getAmount()));
        }
    }
}
