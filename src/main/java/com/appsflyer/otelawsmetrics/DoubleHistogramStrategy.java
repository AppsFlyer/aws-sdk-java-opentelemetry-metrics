package com.appsflyer.otelawsmetrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.metrics.MetricRecord;

public class DoubleHistogramStrategy implements MetricStrategy {
    private static final Logger log = LoggerFactory.getLogger(DoubleHistogramStrategy.class);
    private final DoubleHistogram histogram;

    public DoubleHistogramStrategy(Meter meter, String metricName, String description) {
        this.histogram = meter.histogramBuilder(metricName)
                .setDescription(description)
                .build();
    }

    @Override
    public void record(MetricRecord<?> metricRecord, Attributes attributes) {
        if (metricRecord.value() instanceof Double) {
            Double value = (Double) metricRecord.value();
            histogram.record(value, attributes);
        } else {
            log.warn("Invalid value type for a DoubleHistogram metric: {}", metricRecord.metric().name());
        }
    }
}
