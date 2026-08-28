package com.zippp.metric;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A timer keyed by a fixed set of tag names, with values supplied per call.
 *
 * <p>Sibling of {@link TagCounter} with the same shape: one instance covers the
 * full Cartesian product of {@link #tagNames}, timers are created lazily on
 * the first record call, and values are kept in a thread-safe cache.
 *
 * <p>Two recording styles are supported:
 * <ul>
 *   <li>{@link #start() start()} — returns a {@link Sample}
 *       for inline duration measurement. Stops when {@link Sample#stop(String...)}
 *       is called with the matching tag values.</li>
 *   <li>{@link #record(Duration, String...) record(duration, tags)} — record
 *       an already-known duration (e.g. from a non-Micrometer stopwatch).</li>
 * </ul>
 *
 * <p>Both styles register the same underlying {@link Timer}, so a {@code start}
 * in one request and a {@code record} in another — same name, same tags —
 * share the same metric series.
 */
public class TagTimer {

    private final String name;
    private final String description;
    private final String[] tagNames;
    private final MeterRegistry registry;
    private final Map<List<String>, Timer> timers = new ConcurrentHashMap<>();

    public TagTimer(String name, MeterRegistry registry, String description, String... tagNames) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("metric name must not be blank");
        }
        if (tagNames == null || tagNames.length == 0) {
            throw new IllegalArgumentException("tagNames must contain at least one tag name");
        }
        for (String tagName : tagNames) {
            if (tagName == null || tagName.isBlank()) {
                throw new IllegalArgumentException("tag name must not be blank");
            }
        }
        this.name = name;
        this.registry = registry;
        this.description = description;
        this.tagNames = tagNames.clone();
    }

    /**
     * Start a sample. The actual timer is not registered until {@link Sample#stop(String...)}
     * is called with the outcome tag values.
     */
    public Sample start() {
        return new Sample(registry, this);
    }

    /**
     * Record a duration for the given tag values. Use when the duration is
     * already known (e.g. measured outside Micrometer).
     */
    public void record(Duration duration, String... tagValues) {
        if (duration == null) {
            throw new IllegalArgumentException("duration must not be null");
        }
        validateTagValues(tagValues);
        Timer timer = resolve(sanitizeTagValues(tagValues));
        timer.record(duration);
    }

    /**
     * Total recorded occurrences for the given tag values. Returns 0 if the
     * timer has not yet been started/recorded for that combination.
     */
    public long count(String... tagValues) {
        validateTagValues(tagValues);
        Timer timer = timers.get(sanitizeTagValues(tagValues));
        return timer == null ? 0L : timer.count();
    }

    /**
     * Total recorded time for the given tag values, in nanoseconds. Returns 0
     * if the timer has not yet been started/recorded for that combination.
     */
    public double totalTimeNanos(String... tagValues) {
        validateTagValues(tagValues);
        Timer timer = timers.get(sanitizeTagValues(tagValues));
        return timer == null ? 0.0 : timer.totalTime(TimeUnit.NANOSECONDS);
    }

    /**
     * Snapshot of all currently-registered meter ids for this timer. Useful
     * for diagnostics and tests.
     */
    public List<Meter.Id> getTimerIds() {
        return timers.values().stream()
                .map(Meter::getId)
                .collect(Collectors.toList());
    }

    /**
     * Internal lookup used by {@link Sample#stop(String...)} and
     * {@link #record(Duration, String...)}. Lazy-registers the timer on first use.
     */
    Timer resolve(List<String> sanitizedTagValues) {
        return timers.computeIfAbsent(sanitizedTagValues, values ->
                Timer.builder(name)
                        .description(description != null ? description : "")
                        .tags(toTags(values))
                        .publishPercentileHistogram(true)
                        .register(registry));
    }

    private void validateTagValues(String[] tagValues) {
        if (tagValues == null || tagValues.length != tagNames.length) {
            throw new IllegalArgumentException(
                    "Timer tags mismatch! Expected args are " + Arrays.toString(tagNames)
                            + " (count " + tagNames.length + "), provided tags are "
                            + (tagValues == null ? "null" : Arrays.toString(tagValues))
                            + " (count " + (tagValues == null ? 0 : tagValues.length) + ")");
        }
    }

    private List<String> sanitizeTagValues(String[] tagValues) {
        return TagValues.sanitize(tagValues);
    }

    private List<Tag> toTags(List<String> values) {
        List<Tag> tags = new ArrayList<>(tagNames.length);
        for (int i = 0; i < tagNames.length; i++) {
            tags.add(Tag.of(tagNames[i], values.get(i)));
        }
        return tags;
    }

    /**
     * Stopwatch handle returned by {@link TagTimer#start()}. Records
     * the elapsed time on the underlying timer when {@link #stop(String...)} is
     * called.
     *
     * <p>Callers pass the result tag value to {@code stop} when the outcome
     * of the timed section is known (e.g. a lookup may succeed or throw
     * {@code CustomerNotFoundException}).
     */
    public static final class Sample {
        private final Timer.Sample delegate;
        private final TagTimer owner;

        Sample(MeterRegistry registry, TagTimer owner) {
            this.delegate = Timer.start(registry);
            this.owner = owner;
        }

        /**
         * Stop the sample and record the elapsed duration against the timer
         * for these tag values. Tag values must match the count and order of
         * {@code tagNames} passed to the owning {@link TagTimer}.
         *
         * <p>Does not validate the tag count — callers of {@code Sample} are
         * expected to be typed wrappers that already know the correct tag set
         * (see {@link CustomerLookupTimer.Sample}). Validation lives on the
         * {@link TagTimer}'s public methods ({@link #record}, {@link #count}).
         */
        public void stop(String... stopTagValues) {
            List<String> sanitized = owner.sanitizeTagValues(stopTagValues);
            Timer timer = owner.resolve(sanitized);
            delegate.stop(timer);
        }
    }
}