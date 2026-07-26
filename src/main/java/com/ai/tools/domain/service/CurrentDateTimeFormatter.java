package com.ai.tools.domain.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Formats the current instant from an injected {@link Clock} for LLM tool results.
 */
public final class CurrentDateTimeFormatter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter WEEKDAY =
            DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

    private final Clock clock;
    private final ZoneId defaultZone;

    public CurrentDateTimeFormatter(Clock clock, ZoneId defaultZone) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.defaultZone = Objects.requireNonNull(defaultZone, "defaultZone");
    }

    public String format(String requestedZoneId) {
        ZoneId zone = defaultZone;
        String note = null;
        if (requestedZoneId != null && !requestedZoneId.isBlank()) {
            try {
                zone = ZoneId.of(requestedZoneId.trim());
            } catch (DateTimeException ex) {
                note = "Invalid zoneId '" + requestedZoneId.trim()
                        + "'; fell back to default " + defaultZone.getId() + ".";
            }
        }

        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone);
        StringBuilder out = new StringBuilder();
        out.append("currentDateTime=").append(ISO.format(now)).append('\n');
        out.append("localDate=").append(LOCAL_DATE.format(now)).append('\n');
        out.append("year=").append(now.getYear()).append('\n');
        out.append("dayOfWeek=").append(WEEKDAY.format(now)).append('\n');
        out.append("timeZone=").append(zone.getId()).append('\n');
        out.append("unixEpochMillis=").append(now.toInstant().toEpochMilli());
        if (note != null) {
            out.append('\n').append(note);
        }
        return out.toString();
    }
}
