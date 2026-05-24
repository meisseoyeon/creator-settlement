-- 운영자
INSERT INTO admin (id, name, created_at) VALUES ('admin-1', '플랫폼 운영자', now());

-- 크리에이터
INSERT INTO creator (id, name, created_at) VALUES
                                               ('creator-1', '김강사', now()),
                                               ('creator-2', '이강사', now()),
                                               ('creator-3', '박강사', now());

-- 수강생
INSERT INTO student (id, name, created_at) VALUES
                                               ('student-1', '수강생1', now()),
                                               ('student-2', '수강생2', now()),
                                               ('student-3', '수강생3', now()),
                                               ('student-4', '수강생4', now()),
                                               ('student-5', '수강생5', now()),
                                               ('student-6', '수강생6', now()),
                                               ('student-7', '수강생7', now());

-- 코스
INSERT INTO course (id, creator_id, title, created_at) VALUES
                                                           ('course-1', 'creator-1', 'Spring Boot 입문', now()),
                                                           ('course-2', 'creator-1', 'JPA 실전', now()),
                                                           ('course-3', 'creator-2', 'Kotlin 기초', now()),
                                                           ('course-4', 'creator-3', 'MSA 설계', now());

-- 판매
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at, created_at) VALUES
                                                                                     ('sale-1', 'course-1', 'student-1',  50000, '2025-03-05T10:00:00+09:00', now()),
                                                                                     ('sale-2', 'course-1', 'student-2',  50000, '2025-03-15T14:30:00+09:00', now()),
                                                                                     ('sale-3', 'course-2', 'student-3',  80000, '2025-03-20T09:00:00+09:00', now()),
                                                                                     ('sale-4', 'course-2', 'student-4',  80000, '2025-03-22T11:00:00+09:00', now()),
                                                                                     ('sale-5', 'course-3', 'student-5',  60000, '2025-01-31T23:30:00+09:00', now()),
                                                                                     ('sale-6', 'course-3', 'student-6',  60000, '2025-03-10T16:00:00+09:00', now()),
                                                                                     ('sale-7', 'course-4', 'student-7', 120000, '2025-02-14T10:00:00+09:00', now());

-- 취소(환불)
-- cancel-1: sale-3 전액 환불 80,000 (creator-1 2025-03 환불에 반영)
-- cancel-2: sale-4 부분 환불 30,000 (creator-1 2025-03 환불에 반영)
-- cancel-3: sale-5 환불 60,000 (2025-02 취소 → creator-2 2025-02 환불에 반영)
INSERT INTO cancel_record (id, sale_id, refund_amount, canceled_at, created_at) VALUES
                                                                                    ('cancel-1', 'sale-3', 80000, '2025-03-25T10:00:00+09:00', now()),
                                                                                    ('cancel-2', 'sale-4', 30000, '2025-03-28T15:00:00+09:00', now()),
                                                                                    ('cancel-3', 'sale-5', 60000, '2025-02-02T09:00:00+09:00', now());

-- ===== 추가 검증용 데이터 (creator-4) — 기존 시드에 없던 케이스 =====
-- creator-4 / course-5 신규
INSERT INTO creator (id, name, created_at) VALUES ('creator-4', '최강사', now());
INSERT INTO course (id, creator_id, title, created_at) VALUES
    ('course-5', 'creator-4', 'DB 인덱스 심화', now());

-- 2025-04: (a) 한 판매(sale-8)에 다회 부분 환불, (b) 순 83,333 → 수수료 16,666(소수점 버림 발생)
-- 2025-05: 같은 달 결제+전액환불로 순 판매 정확히 0 (빈 월과 구분)
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at, created_at) VALUES
    ('sale-8',  'course-5', 'student-1', 100000, '2025-04-05T10:00:00+09:00', now()),
    ('sale-9',  'course-5', 'student-2',  33333, '2025-04-20T10:00:00+09:00', now()),
    ('sale-10', 'course-5', 'student-3',  40000, '2025-05-03T10:00:00+09:00', now());

INSERT INTO cancel_record (id, sale_id, refund_amount, canceled_at, created_at) VALUES
    -- sale-8 다회 부분 환불 (누적 50,000 < 원결제 100,000)
    ('cancel-4', 'sale-8',  20000, '2025-04-10T10:00:00+09:00', now()),
    ('cancel-5', 'sale-8',  30000, '2025-04-15T10:00:00+09:00', now()),
    -- sale-10 전액 환불 (같은 5월) → 순 판매 0
    ('cancel-6', 'sale-10', 40000, '2025-05-09T10:00:00+09:00', now());


