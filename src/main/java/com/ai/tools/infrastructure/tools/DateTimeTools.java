package com.ai.tools.infrastructure.tools;

import com.ai.common.domain.repository.DateTimeTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Aligns with Spring AI's official DateTimeTools sample:
 * {@code LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId())}.
 * Uses an injectable {@link Clock} so unit tests can freeze time.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Spring AI Tool Calling</a>
 */
@Component
public class DateTimeTools implements DateTimeTool {

    private final Clock clock;

    public DateTimeTools(Clock clock) {
        this.clock = clock;
    }

    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTime() {
        return Instant.now(clock)
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();
    }
}
