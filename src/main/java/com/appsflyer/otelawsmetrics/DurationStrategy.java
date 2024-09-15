package com.appsflyer.otelawsmetrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.metrics.MetricRecord;

import java.time.Duration;

public class DurationStrategy implements MetricStrategy {
    private static final Logger log = LoggerFactory.getLogger(DurationStrategy.class);
    private final LongHistogram histogram;

    public DurationStrategy(Meter meter, String metricName, String description) {
        this.histogram = meter.histogramBuilder(metricName)
                .setDescription(description)
                .setUnit("ns")
                .ofLongs()
                .build();
    }

    @Override
    public void record(MetricRecord<?> metricRecord, Attributes attributes) {
        if (metricRecord.value() instanceof Duration duration) {
            histogram.record(duration.toNanos(), attributes);
        } else {
            log.warn("Invalid value type for duration metric: {}", metricRecord.metric().name());
        }
    }
}
