package com.ai.tools.infrastructure.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DateTimeTools")
class DateTimeToolsTest {

    @AfterEach
    void clearLocaleContext() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("should return zoned datetime in user timezone when called")
    void shouldReturnZonedDatetimeInUserTimezoneWhenCalled() {
        Instant fixed = Instant.parse("2026-07-26T04:40:00Z");
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.CHINA, TimeZone.getTimeZone(shanghai)));

        DateTimeTools tools = new DateTimeTools(Clock.fixed(fixed, shanghai));

        String result = tools.getCurrentDateTime();

        assertThat(result).isEqualTo("2026-07-26T12:40+08:00[Asia/Shanghai]");
    }

    @Test
    @DisplayName("should use locale context timezone when utc")
    void shouldUseLocaleContextTimezoneWhenUtc() {
        Instant fixed = Instant.parse("2026-07-26T04:40:00Z");
        LocaleContextHolder.setLocaleContext(new SimpleTimeZoneAwareLocaleContext(
                Locale.US, TimeZone.getTimeZone("UTC")));

        DateTimeTools tools = new DateTimeTools(Clock.fixed(fixed, ZoneId.of("UTC")));

        assertThat(tools.getCurrentDateTime()).isEqualTo("2026-07-26T04:40Z[UTC]");
    }
}
