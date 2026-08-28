package com.zippp.metric;

import io.micrometer.core.instrument.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A counter keyed by a fixed set of tag names, with values supplied per call.
 *
 * <p>One {@code TagCounter} instance covers the full Cartesian product of
 * its tag names — counter series are created lazily on the first
 * {@link #increment(String...)} call. This is the right model for
 * high-cardinality tags (e.g. {@code result} × {@code env} × {@code region})
 * where you'd never want to pre-register every possible combination.
 *
 * <p>Thread-safe: backed by a {@link ConcurrentHashMap} so concurrent requests
 * don't lose entries or corrupt the cache. The underlying Micrometer
 * {@code Counter} is also thread-safe.
 */
public class TagCounter {

    private final String name;
    private final String description;
    private final String[] tagNames;
    private final MeterRegistry registry;
    private final Map<List<String>, Counter> counters = new ConcurrentHashMap<>();

    /**
     * @param name        metric name (Micrometer convention: dot-separated, e.g. {@code customer.created}).
     * @param registry    the Micrometer registry to register counters with.
     * @param description human-readable description, exposed as {@code # HELP} in Prometheus.
     * @param tagNames    the fixed tag names this counter accepts. Must be non-empty.
     *                    The order is significant — values passed to {@link #increment} must match.
     */
    public TagCounter(String name, MeterRegistry registry, String description, String... tagNames) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("metric name must not be blank");
        }
        if (tagNames != null) {
            for (String tagName : tagNames) {
                if (tagName == null || tagName.isBlank()) {
                    throw new IllegalArgumentException("tag name must not be blank");
                }
            }
            this.tagNames = tagNames.clone();
        } else {
            this.tagNames = new String[0];
        }

        this.name = name;
        this.registry = registry;
        this.description = description;
    }

    /**
     * Increment by 1 for the given tag values. Values must match the count
     * and order of {@link #tagNames} passed to the constructor.
     *
     * @return the new counter value after incrementing, mainly for tests.
     */
    public double increment(String... tagValues) {
        return increment(1.0, tagValues);
    }

    /**
     * Increment by {@code amount} for the given tag values. Must be non-negative.
     *
     * @return the new counter value after incrementing, mainly for tests.
     */
    public double increment(double amount, String... tagValues) {
        if (amount < 0) {
            throw new IllegalArgumentException("increment amount must be >= 0, got " + amount);
        }
        Counter counter = resolve(tagValues);
        counter.increment(amount);
        return counter.count();
    }

    /**
     * Read the current value for the given tag values without incrementing.
     * Returns 0.0 if the counter has not yet been incremented for that
     * combination (counters are lazy-registered).
     */
    public double count(String... tagValues) {
        Counter counter = counters.get(sanitizeTagValues(tagValues));
        return counter == null ? 0.0 : counter.count();
    }

    /**
     * Snapshot of all currently-registered meter ids for this counter.
     * Useful for diagnostics and tests.
     */
    public List<Meter.Id> getCounterIds() {
        return counters.values().stream()
                .map(Meter::getId)
                .collect(Collectors.toList());
    }

    /**
     * Look up (or lazily create) the {@link Counter} for the given tag values.
     * Validates the tag count before doing any work, so the error message
     * references raw input rather than the {@code UNKNOWN}-normalized form.
     */
    private Counter resolve(String... tagValues) {
        if (tagValues == null || tagValues.length != tagNames.length) {
            throw new IllegalArgumentException(
                    "Counter tags mismatch! Expected args are " + Arrays.toString(tagNames)
                            + " (count " + tagNames.length + "), provided tags are "
                            + (tagValues == null ? "null" : Arrays.toString(tagValues))
                            + " (count " + (tagValues == null ? 0 : tagValues.length) + ")");
        }
        List<String> sanitized = sanitizeTagValues(tagValues);
        // computeIfAbsent serializes creation per tag combination under the
        // ConcurrentHashMap bucket lock — concurrent first-time increments for
        // the same values may both register, but Micrometer's register() is
        // idempotent (returns the existing meter for matching name + tags),
        // so the cache ends up consistent.
        return counters.computeIfAbsent(sanitized, values ->
                Counter.builder(name)
                        .description(description != null ? description : "")
                        .tags(toTags(values))
                        .register(registry)
        );
    }

    /**
     * Replace null/blank tag values with {@code "UNKNOWN"} so a single
     * missing tag doesn't blow up the whole metric. Surfaceable as a
     * queryable anomaly in dashboards.
     */
    private List<String> sanitizeTagValues(String[] tagValues) {
        return TagValues.sanitize(tagValues);
    }

    private List<Tag> toTags(List<String> values) {
        List<Tag> tags = new ArrayList<>(tagNames.length);
        for (int i = 0; i < tagNames.length; i++) {
            tags.add(new ImmutableTag(tagNames[i], values.get(i)));
        }
        return tags;
    }
}