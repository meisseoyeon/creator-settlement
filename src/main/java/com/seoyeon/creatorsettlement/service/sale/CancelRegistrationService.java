package com.seoyeon.creatorsettlement.service.sale;

import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.dto.CancelRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 취소(환불) 등록만 담당한다. 정책 검증은 RefundPolicy 위임. */
@Service
@RequiredArgsConstructor
public class CancelRegistrationService {

    private final SaleRecordRepository saleRepo;
    private final CancelRecordRepository cancelRepo;
    private final RefundPolicy refundPolicy;

    @Transactional
    public CancelRecord register(String saleId, CancelRegisterRequest req) {
        // 판매 행에 쓰기 잠금 → 같은 판매에 대한 동시 환불을 직렬화(환불 누적 검증 보호)
        SaleRecord sale = saleRepo.findByIdForUpdate(saleId)
                .orElseThrow(() -> BusinessException.notFound(
                        "존재하지 않는 판매입니다: " + saleId));

        if (cancelRepo.existsById(req.id())) {
            throw BusinessException.conflict("이미 존재하는 취소 ID입니다: " + req.id());
        }

        refundPolicy.validateTime(sale, req.canceledAt());
        refundPolicy.validateRefundAmount(sale, req.refundAmount());

        CancelRecord cancel = new CancelRecord(
                req.id(), sale, req.refundAmount(), req.canceledAt()
        );
        return cancelRepo.save(cancel);
    }
}
