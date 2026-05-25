package com.jirahourlogger.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NoteParser reads the raw markdown content of a note file and extracts
 * LogEntry objects from it.
 *
 * The format it expects looks like:
 *
 *   09:00 - 11:00
 *
 *   - https://jira.company.com/browse/PROJ-123
 *       - Optional description as a sub-bullet
 *
 *   15:00 - 15:15
 *
 *   - Call w Sander                   ← non-URL bullet becomes description
 *   - https://jira.company.com/browse/PROJ-456
 *
 * This class has only static methods and no state, so its constructor is
 * private to prevent anyone from accidentally creating an instance of it.
 */
public class NoteParser {

    private NoteParser() {}

    // Matches "Date: 07/05/2026" — group (1) is the date string
    private static final Pattern DATE_LINE = Pattern.compile(
            "^Date:\\s*(\\d{2}/\\d{2}/\\d{4})"
    );
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Matches lines like "09:00 - 11:00" or "13:00 - 15:00 " (trailing spaces OK)
    // Groups: (1) start time, (2) end time
    private static final Pattern TIME_RANGE = Pattern.compile(
            "^\\s*(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})\\s*$"
    );

    // Matches a bullet line whose text is a URL:  "- https://..." or "  - https://..."
    // Group: (1) the URL itself
    private static final Pattern URL_BULLET = Pattern.compile(
            "^\\s*-\\s*(https?://\\S+)\\s*$"
    );

    // Matches any bullet line (including non-URL ones like "- Call w Sander")
    // Group: (1) the text after the dash
    private static final Pattern ANY_BULLET = Pattern.compile(
            "^\\s*-\\s*(.+)$"
    );

    /**
     * Scans the content for a "Date: DD/MM/YYYY" line and returns it as a LocalDate.
     * Returns Optional.empty() if no such line is found (e.g. non-daily-note files).
     */
    public static Optional<LocalDate> parseDate(String content) {
        return Arrays.stream(content.split("\\r?\\n"))
                .map(DATE_LINE::matcher)
                .filter(Matcher::matches)
                .findFirst()
                .map(m -> LocalDate.parse(m.group(1), DATE_FMT));
    }

    /**
     * Parses the full text content of a note and returns all log entries found.
     *
     * @param content the raw markdown string
     * @return a list of LogEntry objects, one per time block
     */
    public static List<LogEntry> parse(String content) {
        List<LogEntry> entries = new ArrayList<>();
        String[] lines = content.split("\\r?\\n"); // split on newline, handles Windows (\r\n) too

        int i = 0;
        while (i < lines.length) {
            Matcher timeMatch = TIME_RANGE.matcher(lines[i]);

            if (timeMatch.matches()) {
                // Found the start of a new block — parse the time range
                LocalTime start = LocalTime.parse(timeMatch.group(1));
                LocalTime end   = LocalTime.parse(timeMatch.group(2));
                LogEntry.TimeRange timeRange = new LogEntry.TimeRange(start, end);

                // Advance past the time range line and collect everything in this block
                i++;
                String url = null;
                List<String> descParts = new ArrayList<>();

                // Keep collecting until we hit the next time range line (or end of file)
                while (i < lines.length && !TIME_RANGE.matcher(lines[i]).matches()) {
                    String line    = lines[i];
                    String trimmed = line.trim();

                    Matcher urlMatch    = URL_BULLET.matcher(line);
                    Matcher bulletMatch = ANY_BULLET.matcher(line);

                    if (urlMatch.matches()) {
                        url = urlMatch.group(1);                  // URL bullet
                    } else if (!trimmed.isEmpty()) {
                        // Non-URL bullet → strip the leading "- "
                        // Plain paragraph → use as-is
                        String desc = bulletMatch.matches()
                                ? bulletMatch.group(1).trim()
                                : trimmed;
                        descParts.add(desc);
                    }
                    i++;
                }

                // Only create an entry if we found a URL (time range alone is not enough)
                if (url != null) {
                    Optional<String> description = descParts.isEmpty()
                            ? Optional.empty()
                            : Optional.of(String.join(" ", descParts));
                    entries.add(new LogEntry(timeRange, url, description));
                }
                // i is already pointing at the next time range — don't increment again

            } else {
                i++; // not a time range line — skip it (headers, dates, blank lines, etc.)
            }
        }

        return entries;
    }
}
