package com.appsflyer.otelawsmetrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.metrics.MetricRecord;

public class LongHistogramStrategy implements MetricStrategy {
    private static final Logger log = LoggerFactory.getLogger(LongHistogramStrategy.class);
    private final LongHistogram histogram;

    public LongHistogramStrategy(Meter meter, String metricName, String description) {
        this.histogram = meter.histogramBuilder(metricName)
                .setDescription(description)
                .ofLongs()
                .build();
    }

    @Override
    public void record(MetricRecord<?> metricRecord, Attributes attributes) {
        if (metricRecord.value() instanceof Number) {
            Number value = (Number) metricRecord.value();
            histogram.record(value.longValue(), attributes);
        } else {
            log.warn("Invalid value type for a LongHistogram metric: {}", metricRecord.metric().name());
        }
    }
}
