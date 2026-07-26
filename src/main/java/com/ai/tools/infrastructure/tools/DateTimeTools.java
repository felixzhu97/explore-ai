package com.ai.tools.infrastructure.tools;

import com.ai.common.domain.repository.DateTimeTool;
import com.ai.tools.domain.service.CurrentDateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class DateTimeTools implements DateTimeTool {

    private final CurrentDateTimeFormatter formatter;

    public DateTimeTools(CurrentDateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Tool(description = """
            Return the authoritative current date and time from the system clock.
            Call this BEFORE web search when the user asks about today, now, this year,
            current events, latest stats, or any time-sensitive fact. Use the returned
            year/date in searchWeb queries; never invent the calendar year.""")
    public String getCurrentDateTime(
            @ToolParam(
                    description = "Optional IANA zone id, e.g. Asia/Shanghai or UTC; omit for app default",
                    required = false)
            String zoneId) {
        return formatter.format(zoneId);
    }
}
