package com.appsflyer.otelawsmetrics;

import io.opentelemetry.api.common.Attributes;
import software.amazon.awssdk.metrics.MetricRecord;

@FunctionalInterface
public interface MetricStrategy {
    void record(MetricRecord<?> metricRecord, Attributes attributes);
}
