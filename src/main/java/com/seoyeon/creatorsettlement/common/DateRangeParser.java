package com.seoyeon.creatorsettlement.common;

import com.seoyeon.creatorsettlement.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 'yyyy-MM-dd' 문자열을 KST 기준 OffsetDateTime 범위로 변환한다. */
public final class DateRangeParser {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private DateRangeParser() {}

    /** 'yyyy-MM-dd' → 해당 일자 00:00 KST */
    public static OffsetDateTime parseStartOfDay(String yyyyMMdd) {
        return parse(yyyyMMdd).atStartOfDay(KST).toOffsetDateTime();
    }

    /** 'yyyy-MM-dd' → 다음 일자 00:00 KST (반열린 구간의 exclusive end) */
    public static OffsetDateTime parseEndOfDayExclusive(String yyyyMMdd) {
        return parse(yyyyMMdd).plusDays(1).atStartOfDay(KST).toOffsetDateTime();
    }

    private static LocalDate parse(String yyyyMMdd) {
        try {
            return LocalDate.parse(yyyyMMdd);
        } catch (Exception e) {
            throw BusinessException.badRequest(
                    "잘못된 날짜 형식입니다. yyyy-MM-dd 형식이어야 합니다: " + yyyyMMdd);
        }
    }
}
