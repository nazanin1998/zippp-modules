package com.zippp.metric;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helpers for sanitizing tag values used by {@link TagCounter} and
 * {@link TagTimer}. Package-private — only these two callers should depend
 * on the {@code "UNKNOWN"} fallback semantics.
 */
final class TagValues {

    private TagValues() {
    }

    /**
     * Replace null/blank tag values with {@code "UNKNOWN"} so a single
     * missing tag doesn't blow up the whole metric. Surfaceable as a
     * queryable anomaly in dashboards.
     *
     * <p>A {@code null} array is treated as an empty list — caller's
     * responsibility to validate that the array has the right count before
     * calling this.
     */
    static List<String> sanitize(String[] tagValues) {
        List<String> sanitized = new ArrayList<>(tagValues == null ? 0 : tagValues.length);
        if (tagValues == null) {
            return sanitized;
        }
        for (String tagValue : tagValues) {
            sanitized.add(tagValue == null || tagValue.isBlank() ? "UNKNOWN" : tagValue);
        }
        return sanitized;
    }
}