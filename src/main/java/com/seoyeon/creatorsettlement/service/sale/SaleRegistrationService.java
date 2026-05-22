package com.seoyeon.creatorsettlement.service.sale;

import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import com.seoyeon.creatorsettlement.domain.course.Course;
import com.seoyeon.creatorsettlement.domain.course.CourseRepository;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.dto.SaleRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 판매 등록만 담당한다. */
@Service
@RequiredArgsConstructor
public class SaleRegistrationService {

    private final SaleRecordRepository saleRepo;
    private final CourseRepository courseRepo;

    @Transactional
    public SaleRecord register(SaleRegisterRequest req) {
        if (saleRepo.existsById(req.id())) {
            throw BusinessException.conflict("이미 존재하는 판매 ID입니다: " + req.id());
        }
        Course course = courseRepo.findById(req.courseId())
                .orElseThrow(() -> BusinessException.notFound(
                        "존재하지 않는 강의입니다: " + req.courseId()));

        SaleRecord sale = new SaleRecord(
                req.id(), course, req.studentId(), req.amount(), req.paidAt()
        );
        return saleRepo.save(sale);
    }
}
