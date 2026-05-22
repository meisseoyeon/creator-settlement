package com.seoyeon.creatorsettlement.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonthRangeTest {

    @Test
    @DisplayName("2025-03은 [2025-03-01T00:00+09:00, 2025-04-01T00:00+09:00)")
    void march_2025() {
        MonthRange range = MonthRange.of("2025-03");
        assertThat(range.startInclusive())
                .isEqualTo(OffsetDateTime.parse("2025-03-01T00:00:00+09:00"));
        assertThat(range.endExclusive())
                .isEqualTo(OffsetDateTime.parse("2025-04-01T00:00:00+09:00"));
    }

    @Test
    @DisplayName("월 경계: 3월 1일 00:00은 포함, 4월 1일 00:00은 미포함")
    void boundary_inclusive_exclusive() {
        MonthRange march = MonthRange.of("2025-03");
        assertThat(march.contains(OffsetDateTime.parse("2025-03-01T00:00:00+09:00"))).isTrue();
        assertThat(march.contains(OffsetDateTime.parse("2025-03-31T23:59:59.999+09:00"))).isTrue();
        assertThat(march.contains(OffsetDateTime.parse("2025-04-01T00:00:00+09:00"))).isFalse();
        assertThat(march.contains(OffsetDateTime.parse("2025-02-28T23:59:59+09:00"))).isFalse();
    }

    @Test
    @DisplayName("12월 다음은 다음 해 1월")
    void year_rollover() {
        MonthRange dec = MonthRange.of("2025-12");
        assertThat(dec.endExclusive())
                .isEqualTo(OffsetDateTime.parse("2026-01-01T00:00:00+09:00"));
    }

    @Test
    @DisplayName("잘못된 형식은 IllegalArgumentException")
    void invalid_format() {
        assertThatThrownBy(() -> MonthRange.of("2025-13"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MonthRange.of("25-03"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MonthRange.of("2025/03"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MonthRange.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}