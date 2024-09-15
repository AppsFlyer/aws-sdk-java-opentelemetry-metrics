package com.appsflyer.otelawsmetrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;

/**
 * A metrics reporter that reports AWS SDK metrics to OpenTelemetry.
 * The metric names, descriptions, and units are defined based on <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/metrics-list.html">AWS SDK Metrics List</a>.
 */
public class OtelMetricPublisher implements MetricPublisher {
    private static final Logger log = LoggerFactory.getLogger(OtelMetricPublisher.class);
    private final Map<String, Map<Boolean, Map<Integer, Attributes>>> perRequestAttributesCache = new ConcurrentHashMap<>();
    private final Map<Attributes, Map<String, Attributes>> perAttemptAttributesCache = new ConcurrentHashMap<>();
    private final Map<Attributes, Map<Integer, Attributes>> perHttpAttributesCache = new ConcurrentHashMap<>();

    private final Executor executor;
    private final String metricPrefix;
    private final Map<String, MetricStrategy> perRequestMetrics;
    private final Map<String, MetricStrategy> perAttemptMetrics;
    private final Map<String, MetricStrategy> httpMetrics;

    public OtelMetricPublisher(OpenTelemetry openTelemetry, String metricPrefix, Executor executor) {
        this.metricPrefix = metricPrefix;
        if (executor == null) {
            log.warn("An internal executor is not provided. This may impact the performance of the application. Falling back to the common ForkJoinPool.");
            this.executor = ForkJoinPool.commonPool();
        } else {
            this.executor = executor;
        }

        Meter meter = openTelemetry.getMeter("aws.sdk");

        perRequestMetrics = initializePerRequestStrategies(meter);
        perAttemptMetrics = initializeCoreStrategies(meter);
        httpMetrics = initializeHttpStrategies(meter);
    }

    @Override
    public void publish(MetricCollection metricCollection) {
        try {
            executor.execute(() -> publishInternal(metricCollection));
        } catch (RejectedExecutionException ex) {
            log.warn("Some AWS SDK client-side metrics have been dropped because an internal executor did not accept the task.", ex);
        }
    }

    @Override
    public void close() {
        // This publisher does not allocate any resources that need to be cleaned up.
    }

    private Map<String, MetricStrategy> initializePerRequestStrategies(Meter meter) {
        return Map.of(CoreMetric.API_CALL_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".api_call_duration",
                        "The total time taken to finish a request (inclusive of all retries)")),

                CoreMetric.CREDENTIALS_FETCH_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".credentials_fetch_duration",
                        "The time taken to fetch AWS signing credentials for the request")),

                CoreMetric.ENDPOINT_RESOLVE_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".endpoint_resolve_duration",
                        "The duration of time it took to resolve the endpoint used for the API call")),

                CoreMetric.MARSHALLING_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".marshalling_duration",
                        "The time it takes to marshall an SDK request to an HTTP request")),

                CoreMetric.TOKEN_FETCH_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".token_fetch_duration",
                        "The time taken to fetch token signing credentials for the request")));
    }

    private Map<String, MetricStrategy> initializeCoreStrategies(Meter meter) {

        return Map.of(CoreMetric.BACKOFF_DELAY_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".backoff_delay_duration",
                        "The duration of time the SDK waited before this API call attempt")),

                CoreMetric.READ_THROUGHPUT.name(), new MetricStrategyWithoutErrors(new DoubleHistogramStrategy(meter,
                        metricPrefix + ".read_throughput",
                        "The read throughput of the client in bytes/second")),

                CoreMetric.SERVICE_CALL_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".service_call_duration",
                        "The time it takes to connect to the service, send the request, and receive the HTTP status code and header from the response")),

                CoreMetric.SIGNING_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".signing_duration",
                        "The time it takes to sign the HTTP request")),

                CoreMetric.TIME_TO_FIRST_BYTE.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".time_to_first_byte",
                        "Elapsed time from sending the HTTP request (including acquiring a connection) to receiving the first byte of the headers in the response")),

                CoreMetric.TIME_TO_LAST_BYTE.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".time_to_last_byte",
                        "Elapsed time from sending the HTTP request (including acquiring a connection) to receiving the last byte of the response")),

                CoreMetric.UNMARSHALLING_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".unmarshalling_duration",
                        "The time it takes to unmarshall an HTTP response to an SDK response")));
    }

    private Map<String, MetricStrategy> initializeHttpStrategies(Meter meter) {
        return Map.of(HttpMetric.AVAILABLE_CONCURRENCY.name(), new MetricStrategyWithoutErrors(new LongHistogramStrategy(meter,
                        metricPrefix + ".available_concurrency",
                        "The number of remaining concurrent requests that can be supported by the HTTP client without needing to establish another connection")),

                HttpMetric.CONCURRENCY_ACQUIRE_DURATION.name(), new MetricStrategyWithoutErrors(new DurationStrategy(meter,
                        metricPrefix + ".concurrency_acquire_duration",
                        "The time taken to acquire a channel from the connection pool")),

                HttpMetric.LEASED_CONCURRENCY.name(), new MetricStrategyWithoutErrors(new LongHistogramStrategy(meter,
                        metricPrefix + ".leased_concurrency",
                        "The number of request currently being executed by the HTTP client")),

                HttpMetric.MAX_CONCURRENCY.name(), new MetricStrategyWithoutErrors(new LongHistogramStrategy(meter,
                        metricPrefix + ".max_concurrency",
                        "The max number of concurrent requests supported by the HTTP client")),

                HttpMetric.PENDING_CONCURRENCY_ACQUIRES.name(), new MetricStrategyWithoutErrors(new LongHistogramStrategy(meter,
                        metricPrefix + ".pending_concurrency_acquires",
                        "The number of requests that are blocked, waiting for another TCP connection or a new stream to be available from the connection pool")));
    }

    private void publishInternal(MetricCollection metricCollection) {
        try {
            // Start processing from the root per-request metrics
            processPerRequestMetrics(metricCollection);
        } catch (Exception e) {
            log.error("An error occurred while publishing metrics", e);
        }
    }

    private void recordMetrics(Map<String, MetricRecord<?>> metricsMap,
                               Attributes attributes,
                               Map<String, MetricStrategy> metricStrategies) {
        for (Map.Entry<String, MetricStrategy> entry : metricStrategies.entrySet()) {
            MetricRecord<?> metricRecord = metricsMap.get(entry.getKey());
            if (metricRecord != null) {
                entry.getValue().record(metricRecord, attributes);
            }
        }
    }

    private void processPerRequestMetrics(MetricCollection requestMetrics) {
        Map<String, MetricRecord<?>> metricsMap = extractMetrics(requestMetrics);

        // Extract attributes for per-request metrics
        String operationName = getStringMetricValue(metricsMap, CoreMetric.OPERATION_NAME.name());
        boolean isSuccess = getBooleanMetricValue(metricsMap, CoreMetric.API_CALL_SUCCESSFUL.name());
        int retryCount = getIntMetricValue(metricsMap, CoreMetric.RETRY_COUNT.name());
        Attributes attributes = toPerRequestAttributes(operationName, isSuccess, retryCount);

        // Report per-request metrics
        recordMetrics(metricsMap, attributes, perRequestMetrics);

        // Process per-attempt metrics
        for (MetricCollection attemptMetrics : requestMetrics.children()) {
            processPerAttemptMetrics(attemptMetrics, attributes);
        }
    }

    private void processPerAttemptMetrics(MetricCollection attemptMetrics, Attributes parentAttributes) {
        Map<String, MetricRecord<?>> metricsMap = extractMetrics(attemptMetrics);

        // Extract ErrorType if present
        String errorType = getStringMetricValue(metricsMap, CoreMetric.ERROR_TYPE.name());

        // Build attributes including attempt number and error type
        Attributes attributes = toAttemptAttributes(parentAttributes, errorType);

        // Report per-attempt metrics
        recordMetrics(metricsMap, attributes, perAttemptMetrics);

        // Process HTTP metrics
        for (MetricCollection httpMetricsCollection : attemptMetrics.children()) {
            processHttpMetrics(httpMetricsCollection, attributes);
        }
    }

    private void processHttpMetrics(MetricCollection httpMetricsCollection, Attributes parentAttributes) {
        Map<String, MetricRecord<?>> metricsMap = extractMetrics(httpMetricsCollection);

        // Extract HTTP status code
        int httpStatusCode = getIntMetricValue(metricsMap, HttpMetric.HTTP_STATUS_CODE.name());
        Attributes attributes = toHttpAttributes(parentAttributes, httpStatusCode);

        // Report HTTP metrics
        recordMetrics(metricsMap, attributes, httpMetrics);
    }

    private Map<String, MetricRecord<?>> extractMetrics(MetricCollection metricCollection) {
        Map<String, MetricRecord<?>> metricMap = new HashMap<>();
        for (MetricRecord<?> metricRecord : metricCollection) {
            metricMap.put(metricRecord.metric().name(), metricRecord);
        }
        return metricMap;
    }

    private String getStringMetricValue(Map<String, MetricRecord<?>> metricsMap, String metricName) {
        MetricRecord<?> metricRecord = metricsMap.get(metricName);
        if (metricRecord != null) {
            Object value = metricRecord.value();
            if (value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean getBooleanMetricValue(Map<String, MetricRecord<?>> metricsMap, String metricName) {
        MetricRecord<?> metricRecord = metricsMap.get(metricName);
        if (metricRecord != null) {
            Object value = metricRecord.value();
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return false;
    }

    private int getIntMetricValue(Map<String, MetricRecord<?>> metricsMap, String metricName) {
        MetricRecord<?> metricRecord = metricsMap.get(metricName);
        if (metricRecord != null) {
            Object value = metricRecord.value();
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return 0;
    }

    private Attributes toPerRequestAttributes(String operationName, boolean isSuccess, int retryCount) {
        String nullSafeOperationName = operationName == null ? "null" : operationName;
        return perRequestAttributesCache
                .computeIfAbsent(nullSafeOperationName, op -> new ConcurrentHashMap<>())
                .computeIfAbsent(isSuccess, success -> new ConcurrentHashMap<>())
                .computeIfAbsent(retryCount, rc -> Attributes.builder()
                        .put("request_operation_name", nullSafeOperationName)
                        .put("request_is_success", isSuccess)
                        .put("request_retry_count", retryCount)
                        .build());
    }

    private Attributes toAttemptAttributes(Attributes parentAttributes, String errorType) {
        String safeErrorType = errorType == null ? "no_error" : errorType;
        return perAttemptAttributesCache
                .computeIfAbsent(parentAttributes, attr -> new ConcurrentHashMap<>())
                .computeIfAbsent(safeErrorType, type ->
                        Attributes.builder()
                                .putAll(parentAttributes)
                                .put("attempt_error_type", type)
                                .build());
    }

    private Attributes toHttpAttributes(Attributes parentAttributes, int httpStatusCode) {
        return perHttpAttributesCache
                .computeIfAbsent(parentAttributes, attr -> new ConcurrentHashMap<>())
                .computeIfAbsent(httpStatusCode, code ->
                        Attributes.builder()
                                .putAll(parentAttributes)
                                .put("http_status_code", code)
                                .build());
    }
}

