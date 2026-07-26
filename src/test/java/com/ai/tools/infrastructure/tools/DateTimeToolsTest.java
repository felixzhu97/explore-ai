package com.ai.tools.infrastructure.tools;

import com.ai.tools.domain.service.CurrentDateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DateTimeTools")
class DateTimeToolsTest {

    @Test
    @DisplayName("should_return_formatted_clock_when_getCurrentDateTime_called")
    void should_return_formatted_clock_when_getCurrentDateTime_called() {
        Instant fixed = Instant.parse("2026-07-26T04:40:00Z");
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        DateTimeTools tools = new DateTimeTools(
                new CurrentDateTimeFormatter(Clock.fixed(fixed, zone), zone));

        String result = tools.getCurrentDateTime(null);

        assertThat(result).contains("year=2026");
        assertThat(result).contains("localDate=2026-07-26");
        assertThat(result).contains("timeZone=Asia/Shanghai");
    }
}
