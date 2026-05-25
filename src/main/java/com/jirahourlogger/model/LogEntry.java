package com.jirahourlogger.model;

import java.time.LocalTime;
import java.util.Optional;

/**
 * LogEntry groups all the data for a single logged work block:
 * - timeRange   → when the work happened (start + end)
 * - url         → the Jira ticket URL
 * - description → optional extra notes about the work
 *
 * TimeRange is a nested record — it only makes sense in the context
 * of a LogEntry, so keeping it nested avoids polluting the package.
 *
 * Optional<String> means the description may or may not be present.
 * Callers must explicitly unwrap it, which prevents accidental NullPointerExceptions.
 * Use Optional.of("text") when there is a value, Optional.empty() when there isn't.
 */
public record LogEntry(TimeRange timeRange, String url, Optional<String> description) {

    public record TimeRange(LocalTime start, LocalTime end) {}
}
