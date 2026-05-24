package com.seoyeon.creatorsettlement.service;

import com.seoyeon.creatorsettlement.common.exception.BusinessException;
import com.seoyeon.creatorsettlement.domain.course.Course;
import com.seoyeon.creatorsettlement.domain.course.CourseRepository;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecord;
import com.seoyeon.creatorsettlement.domain.sale.CancelRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecord;
import com.seoyeon.creatorsettlement.domain.sale.SaleRecordRepository;
import com.seoyeon.creatorsettlement.domain.sale.dto.CancelRegisterRequest;
import com.seoyeon.creatorsettlement.service.sale.CancelRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 환불 경합(race) 검증.
 * 비관적 락(SELECT ... FOR UPDATE)으로 같은 판매의 동시 환불이 직렬화되어
 * "환불 누적 ≤ 원결제" 불변식이 깨지지 않는지 확인한다.
 *
 * 실제 커밋과 두 개의 동시 트랜잭션이 필요하므로 @Transactional(롤백) 을 쓰지 않고,
 * 테스트가 만든 데이터는 @AfterEach 에서 직접 정리한다.
 */
@SpringBootTest
class CancelConcurrencyTest {

    @Autowired CancelRegistrationService cancelService;
    @Autowired SaleRecordRepository saleRepo;
    @Autowired CancelRecordRepository cancelRepo;
    @Autowired CourseRepository courseRepo;

    private static final String SALE_ID = "sale-concurrency";
    private static final long SALE_AMOUNT = 50_000L;
    private static final long REFUND_EACH = 40_000L;  // 2건이면 80,000 > 50,000 → 하나만 성공해야 함

    @BeforeEach
    void setUp() {
        Course course = courseRepo.findById("course-1").orElseThrow();
        saleRepo.save(new SaleRecord(
                SALE_ID, course, "student-1", SALE_AMOUNT,
                OffsetDateTime.parse("2025-03-01T10:00:00+09:00")));
    }

    @AfterEach
    void tearDown() {
        cancelRepo.deleteAll(cancelRepo.findBySale_Id(SALE_ID));
        saleRepo.deleteById(SALE_ID);
    }

    @Test
    @DisplayName("동시 환불 40,000원 2건 → 비관적 락으로 직렬화되어 1건만 성공, 누적 ≤ 원결제")
    void concurrentRefunds_onlyOneSucceeds() throws Exception {
        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);   // 동시에 출발시키는 신호
        CountDownLatch finished = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            String cancelId = "cancel-concurrency-" + i;
            pool.submit(() -> {
                try {
                    ready.await();
                    cancelService.register(SALE_ID, new CancelRegisterRequest(
                            cancelId, REFUND_EACH,
                            OffsetDateTime.parse("2025-03-02T10:00:00+09:00")));
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    rejected.incrementAndGet();   // 누적 초과로 거부된 정상 케이스
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        ready.countDown();                          // 두 스레드 동시 출발
        finished.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        long totalRefunded = cancelRepo.findBySale_Id(SALE_ID).stream()
                .mapToLong(CancelRecord::getRefundAmount)
                .sum();

        assertThat(success.get()).isEqualTo(1);          // 정확히 1건만 성공
        assertThat(rejected.get()).isEqualTo(1);         // 나머지 1건은 거부
        assertThat(totalRefunded).isLessThanOrEqualTo(SALE_AMOUNT);  // 불변식 보존
    }
}
