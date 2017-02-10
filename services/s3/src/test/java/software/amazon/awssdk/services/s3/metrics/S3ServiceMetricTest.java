package software.amazon.awssdk.services.s3.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.ServiceMetricCollector;
import software.amazon.awssdk.metrics.ServiceMetricType;
import software.amazon.awssdk.metrics.ThroughputMetricType;
import software.amazon.awssdk.metrics.internal.ServiceMetricTypeGuesser;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * This test ensures the {@link ServiceMetricTypeGuesser} behaves consistently
 * with the {@link S3ServiceMetric}.
 */
public class S3ServiceMetricTest {
    @Test
    public void ensuresConsistentBehavior() {
        PutObjectRequest orig = new PutObjectRequest("", "", "");
        final String expectedServiceName = AmazonS3Client.S3_SERVICE_NAME;
        Request<?> req = new DefaultRequest<PutObjectRequest>(orig, expectedServiceName);
        for (S3ServiceMetric expectedType: S3ServiceMetric.values()) {
            final String expectedTypeName = expectedType.name();
            final String suffix = expectedTypeName.substring(2);
            if (expectedType instanceof ThroughputMetricType) {
                ThroughputMetricType expectedThroughputType = (ThroughputMetricType)expectedType;
                ServiceMetricType expectedByteCountType = expectedThroughputType.getByteCountMetricType();
                final String expectedByteCountTypeName = expectedByteCountType.name();
                final String byteCountTypeNameSuffix = expectedByteCountTypeName.substring(2);
                ThroughputMetricType actualType = ServiceMetricTypeGuesser
                        .guessThroughputMetricType(req, suffix, byteCountTypeNameSuffix);
                // by default the metrics is disabled
                if (!AwsSdkMetrics.isMetricsEnabled()) {
                    assertNull(actualType);
                    // set to a custom collector, so now considered enabled
                    AwsSdkMetrics.setMetricCollector(new MetricCollector() {
                        @Override public boolean start() { return true; }
                        @Override public boolean stop() { return false; }
                        @Override public boolean isEnabled() { return true; }
                        @Override
                        public RequestMetricCollector getRequestMetricCollector() {
                            return RequestMetricCollector.NONE;
                        }
                        @Override
                        public ServiceMetricCollector getServiceMetricCollector() {
                            return ServiceMetricCollector.NONE;
                        }
                    });
                    actualType = ServiceMetricTypeGuesser
                            .guessThroughputMetricType(req, suffix, byteCountTypeNameSuffix);
                }
                String actualTypeName = actualType.name();
                final ServiceMetricType actualByteCountType = actualType.getByteCountMetricType();
                assertEquals(expectedServiceName, actualType.getServiceName());
                assertEquals(expectedServiceName, actualByteCountType.getServiceName());
                assertEquals(actualTypeName, expectedType, actualType);
                final String actualByteCountTypeName = actualByteCountType.name();
                assertEquals(actualByteCountTypeName, expectedByteCountType, actualByteCountType);
                assertFalse(actualTypeName, actualType.equals(expectedByteCountType));
                S3ServiceMetric.valueOf(actualByteCountTypeName);
                S3ServiceMetric.valueOf(actualTypeName);
            }
        }
    }
}
