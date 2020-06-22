# Metrics Meeting

### New Modules

 - [Metrics SPI](https://github.com/aws/aws-sdk-java-v2/tree/sdk-metrics-development-2/core/metrics-spi)

    - Sub module of `core`
 - [Metric Publishers](https://github.com/aws/aws-sdk-java-v2/tree/sdk-metrics-development-2/metric-publishers)

    - new top-level module containing different metric publisher implements

    - Current out-of-the-box implementation is [Cloudwatch Publisher](https://github.com/aws/aws-sdk-java-v2/tree/sdk-metrics-development-2/metric-publishers/cloudwatch-metric-publisher)


### Configuration

Current way is to set the desired publisher on [`ClientOverrideConfiguration`](https://github.com/aws/aws-sdk-java-v2/blob/3d57c583a56c11def229349b01f6c3b2d9fe919a/core/sdk-core/src/main/java/software/amazon/awssdk/core/client/config/ClientOverrideConfiguration.java#L430) and [`RequestOverrideConfiguration`](https://github.com/aws/aws-sdk-java-v2/blob/3d57c583a56c11def229349b01f6c3b2d9fe919a/core/sdk-core/src/main/java/software/amazon/awssdk/core/RequestOverrideConfiguration.java#L362). Request override always takes precedence.

Complete configuration story in progress

### Collected Metrics

 - [Updated Metrics list](https://github.com/aws/aws-sdk-java-v2/blob/5cdccd9bfe6f934cceaaab62d7cc2bbe99d58e2a/docs/design/core/metrics/MetricsList.md)

   In the code, the metrics are contained and grouped within constant classes to easily find them.

 - Constants classes

   - [CoreMetric](https://github.com/aws/aws-sdk-java-v2/blob/8c192e3b04892987bf0872f76ba4f65167f3a872/core/sdk-core/src/main/java/software/amazon/awssdk/core/metrics/CoreMetric.java#L24)

     Metrics collected within the core of the SDK e.g. marshalling and signing metrics.

   - [HttpMetric](https://github.com/aws/aws-sdk-java-v2/tree/sdk-metrics-development-2/core/metrics-spi)


     Metrics collected by HTTP/1.1 and HTTP/2 clients. Level of support depends on HTTP library used.

   - [Http2Metric](https://github.com/aws/aws-sdk-java-v2/blob/sdk-metrics-development-2/http-client-spi/src/main/java/software/amazon/awssdk/http/Http2Metric.java)

      Metrics exclusive to and HTTP/2 clients. Level of support depends on HTTP library used.


### Interfaces
 - [`SdkMetric`](https://github.com/aws/aws-sdk-java-v2/blob/sdk-metrics-development-2/core/metrics-spi/src/main/java/software/amazon/awssdk/metrics/SdkMetric.java)
 - [`MetricCategory`](https://github.com/aws/aws-sdk-java-v2/blob/sdk-metrics-development-2/core/metrics-spi/src/main/java/software/amazon/awssdk/metrics/MetricCategory.java)
 - [`MetricCollector`](https://github.com/aws/aws-sdk-java-v2/blob/sdk-metrics-development-2/core/metrics-spi/src/main/java/software/amazon/awssdk/metrics/MetricCollector.java)
 - [`MetricCollection`](https://github.com/aws/aws-sdk-java-v2/blob/sdk-metrics-development-2/core/metrics-spi/src/main/java/software/amazon/awssdk/metrics/MetricCollection.java)
 - [`MetricPublisher`](https://github.com/aws/aws-sdk-java-v2/blob/sdk-metrics-development-2/core/metrics-spi/src/main/java/software/amazon/awssdk/metrics/MetricPublisher.java)
 - [`MetricRecord`](https://github.com/aws/aws-sdk-java-v2/blob/sdk-metrics-development-2/core/metrics-spi/src/main/java/software/amazon/awssdk/metrics/MetricRecord.java)