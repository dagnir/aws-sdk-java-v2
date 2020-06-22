package com.dongieagnir;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.utils.Logger;

public final class MyLoggingPublisher implements MetricPublisher {
    private static final Logger LOG = Logger.loggerFor("com.dongieagnir.metrics");

    @Override
    public void publish(MetricCollection metricCollection) {
        LOG.info(() -> makeLogString(metricCollection));
    }

    @Override
    public void close() {
    }

    private static String makeLogString(MetricCollection apiCallMetrics) {
        StringBuilder builder = new StringBuilder();

        // Iteration over records at the level of the collection
        builder.append("Overall call metrics: {");
        for (MetricRecord<?> record : apiCallMetrics) {
            // Every metric is associated with a category to help with filtering
            if (record.metric().categories().contains(MetricCategory.DEFAULT)) {
                builder.append(recordToString(record)).append(" ");
            }
        }

        builder.append("}, Http Metrics: {");

        // Child collections are accessed through children() getter.
        //
        // Metric collections are hierarchical: A full API collection tree looks like: ApiCall -> ApiCallAttempt * -> HttpMetric *
        List<MetricCollection> httpMetricCollections = apiCallMetrics.children().stream()
                .flatMap(c -> c.children().stream())
                // Collections are named
                .filter(c -> c.name().equalsIgnoreCase("HttpClient"))
                .collect(Collectors.toList());

        for (MetricCollection httpMetrics : httpMetricCollections) {
            // Specific metric values accessed through metricValues()
            // Note that they're typed
            List<Integer> leasedConcurrency = httpMetrics.metricValues(HttpMetric.LEASED_CONCURRENCY);
            builder.append("leased: ").append(leasedConcurrency.get(0));
        }

        builder.append(" }, ");

        // Order of creation for metrics and and children is preserved
        List<MetricCollection> attemptMetrics = apiCallMetrics.children().stream()
                .filter(c -> c.name().equalsIgnoreCase("ApiCallAttempt"))
                .collect(Collectors.toList());

        // SdkMetric is typed on the value, similar to AttributeKey
        List<Integer> httpStatusCodes = attemptMetrics.get(attemptMetrics.size() - 1)
                .metricValues(CoreMetric.HTTP_STATUS_CODE);

        int lastHttpStatus = httpStatusCodes.get(httpStatusCodes.size() - 1);

        builder.append("Last status: ").append(lastHttpStatus);

        return builder.append("}").toString();
    }

    private static String recordToString(MetricRecord<?> record) {
        return record.metric().name() + "=" + record.value();
    }
}
