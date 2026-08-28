package com.zippp.metric;

import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TagGauge {

    private final String name;
    private final String[] tagNames;
    private final MeterRegistry registry;
    private final Map<String, AtomicInteger> values = new HashMap<>();

    public TagGauge(String name, MeterRegistry registry, String... tags) {
        this.name = name;
        this.registry = registry;
        this.tagNames = tags;
    }

    public void set(int newValue, String... tagValues) {
        this.getGaugeValue(tagValues).set(newValue);
    }

    public void increment(String... tagValues) {
        this.getGaugeValue(tagValues).incrementAndGet();
    }

    public void decrement(String... tagValues) {
        this.getGaugeValue(tagValues).decrementAndGet();
    }

    public int get(String... tagValues) {
        return this.getGaugeValue(tagValues).get();
    }

    private AtomicInteger getGaugeValue(String... tagValues) {
        List<String> tagValueList = Arrays.stream(tagValues).map(tagValue -> {
            if (Objects.isNull(tagValue) || StringUtils.isBlank(tagValue)) {
                return "UNKNOWN";
            }
            return tagValue;
        }).collect(Collectors.toList());

        if (tagValues.length != tagNames.length) {
            throw new IllegalArgumentException("Gauge tags mismatch! Expected args are " + Arrays.toString(tagNames) +
                ", provided tags are " + tagValueList);
        }

        String valuesKey = String.join("-", tagValueList);

        return values.computeIfAbsent(valuesKey, key -> {
            AtomicInteger value = new AtomicInteger(0);
            List<Tag> tags = new ArrayList<>(tagNames.length);
            for (int i = 0; i < tagNames.length; i++) {
                tags.add(new ImmutableTag(tagNames[i], tagValueList.get(i)));
            }
            Gauge.builder(name, value, AtomicInteger::get)
                .tags(tags)
                .register(registry);
            return value;
        });
    }

    public List<Meter.Id> getGaugeIds() {
        return this.registry.get(name).meters()
            .stream()
            .map(Meter::getId)
            .collect(Collectors.toList());
    }
}
