package com.appsflyer.otelawsmetrics;

import io.opentelemetry.api.common.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.metrics.MetricRecord;

/**
 * A {@link MetricStrategy} that delegates to another {@link MetricStrategy} and catches any exceptions that occur
 * during the delegation. If an exception occurs, it logs a warning and continues.
 */
public class MetricStrategyWithoutErrors implements MetricStrategy {
    private static final Logger log = LoggerFactory.getLogger(MetricStrategyWithoutErrors.class);

    private final MetricStrategy delegate;

    public MetricStrategyWithoutErrors(MetricStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public void record(MetricRecord<?> metricRecord, Attributes attributes) {
        if (metricRecord == null) {
            log.warn("Received null metric record");
            return;
        }

        try {
            delegate.record(metricRecord, attributes);
        } catch (Exception e) {
            String metricName = metricRecord.metric() == null ? "null" : metricRecord.metric().name();
            log.warn("Failed to record metric: {}", metricName, e);
        }
    }
}
