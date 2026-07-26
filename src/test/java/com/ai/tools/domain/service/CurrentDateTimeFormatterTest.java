package com.ai.tools.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CurrentDateTimeFormatter")
class CurrentDateTimeFormatterTest {

    private static final Instant FIXED = Instant.parse("2026-07-26T04:40:00Z");
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final CurrentDateTimeFormatter formatter =
            new CurrentDateTimeFormatter(Clock.fixed(FIXED, SHANGHAI), SHANGHAI);

    @Nested
    @DisplayName("format")
    class Format {

        @Test
        @DisplayName("should_return_shanghai_local_date_when_zone_omitted")
        void should_return_shanghai_local_date_when_zone_omitted() {
            String result = formatter.format(null);

            assertThat(result).contains("localDate=2026-07-26");
            assertThat(result).contains("year=2026");
            assertThat(result).contains("timeZone=Asia/Shanghai");
            assertThat(result).contains("dayOfWeek=Sunday");
            assertThat(result).contains("unixEpochMillis=" + FIXED.toEpochMilli());
        }

        @Test
        @DisplayName("should_use_requested_zone_when_valid")
        void should_use_requested_zone_when_valid() {
            String result = formatter.format("UTC");

            assertThat(result).contains("localDate=2026-07-26");
            assertThat(result).contains("timeZone=UTC");
            assertThat(result).contains("currentDateTime=2026-07-26T04:40:00Z");
        }

        @Test
        @DisplayName("should_fall_back_to_default_zone_when_zone_invalid")
        void should_fall_back_to_default_zone_when_zone_invalid() {
            String result = formatter.format("Not/AZone");

            assertThat(result).contains("timeZone=Asia/Shanghai");
            assertThat(result).contains("Invalid zoneId 'Not/AZone'");
            assertThat(result).contains("fell back to default Asia/Shanghai");
        }
    }
}
