package com.seoyeon.creatorsettlement.common;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * KST 기준 월 경계를 표현한다.
 * 반열린 구간 [startInclusive, endExclusive) 로 표현해 경계 중복을 방지.
 */
public record MonthRange(OffsetDateTime startInclusive, OffsetDateTime endExclusive) {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** "2025-03" 형식의 문자열을 받아 KST 월 경계를 만든다. */
    public static MonthRange of(String yearMonth) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonth, MONTH_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "잘못된 연월 형식입니다. 예: 2025-03, 입력값: " + yearMonth);
        }
        LocalDate firstDay = ym.atDay(1);
        LocalDate nextMonthFirstDay = ym.plusMonths(1).atDay(1);
        return new MonthRange(
                firstDay.atStartOfDay(KST).toOffsetDateTime(),
                nextMonthFirstDay.atStartOfDay(KST).toOffsetDateTime()
        );
    }

    /** 해당 일시가 이 월 범위에 속하는지. */
    public boolean contains(OffsetDateTime time) {
        return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
    }
}