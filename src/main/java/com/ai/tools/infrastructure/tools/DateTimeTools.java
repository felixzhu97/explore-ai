package com.ai.tools.infrastructure.tools;

import com.ai.common.domain.repository.DateTimeTool;
import java.time.Clock;
import java.time.Instant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aligns with Spring AI's official DateTimeTools sample: {@code
 * LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId())}. Uses an injectable
 * {@link Clock} so unit tests can freeze time.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Spring AI Tool
 *     Calling</a>
 */
@Component
public class DateTimeTools implements DateTimeTool {

  private final Clock clock;

  /** Documentation. */
  public DateTimeTools(Clock clock) {
    this.clock = clock;
  }

  @Tool(
      description =
          """
            Authoritative current date/time in the user's timezone. Call before any
            time-sensitive web search (today, this year, latest, current events) and
            use the returned year/date in queries; never invent the calendar year.""")
  public String getCurrentDateTime() {
    return Instant.now(clock).atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
  }
}
