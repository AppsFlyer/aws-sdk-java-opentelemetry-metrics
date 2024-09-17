# AWS SDK Java OpenTelemetry Metrics

A lightweight metrics publisher that integrates AWS SDK metrics with OpenTelemetry, allowing you to monitor and collect
AWS client performance metrics in your distributed applications.

## Usage

This library integrates AWS SDK Java metrics with OpenTelemetry’s metrics API, allowing you to collect and publish AWS client performance data such as API call durations, retry counts, and more.

### Basic Example

Here’s a simple example of how to use the `OtelMetricPublisher`:

```java
import com.appsflyer.otelawsmetrics.OtelMetricPublisher;
import io.opentelemetry.api.OpenTelemetry;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.metrics.MetricPublisher;

public class MyAwsService {
    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    public MyAwsService(OpenTelemetry openTelemetry) {
        // Create the metric publisher
        MetricPublisher metricPublisher = new OtelMetricPublisher(openTelemetry, "aws.sdk");

        // Create the DynamoDbAsyncClient with the metric publisher
        this.dynamoDbAsyncClient = DynamoDbAsyncClient.builder()
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .addMetricPublisher(metricPublisher)
                        .build())
                .build();
    }

    public void putItemAsync(String tableName, Map<String, AttributeValue> item) {
        // Perform DynamoDB operations and automatically collect metrics
        dynamoDbAsyncClient.putItem(putItemRequest -> putItemRequest.tableName(tableName).item(item));
    }
}
```

### Configuration

You can configure the OtelMetricPublisher with additional options if needed:

```java
Executor customExecutor = Executors.newSingleThreadExecutor();
OtelMetricPublisher metricPublisher = new OtelMetricPublisher(OpenTelemetry.get(), customExecutor);
```

This allows you to use a custom executor for asynchronous metrics publishing.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

