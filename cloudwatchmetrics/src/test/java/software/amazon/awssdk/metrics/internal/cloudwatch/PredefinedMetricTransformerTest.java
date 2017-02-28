package software.amazon.awssdk.metrics.internal.cloudwatch;
import static software.amazon.awssdk.metrics.internal.cloudwatch.PredefinedMetricTransformer.EXCLUDE_REQUEST_TYPE;
import static software.amazon.awssdk.metrics.internal.cloudwatch.PredefinedMetricTransformer.INCLUDE_REQUEST_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.AwsMetricTransformerFactory;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.Dimensions;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.RequestMetricTransformer;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.util.AwsRequestMetrics;
import software.amazon.awssdk.util.AwsRequestMetrics.Field;
import software.amazon.awssdk.util.AwsRequestMetricsFullSupport;
import software.amazon.awssdk.util.TimingInfo;

/**
 * This is an important unit test used to safe-guard against accidental
 * mis-configuration of the service specific request metric transformers which
 * are dynamically loaded from @link {@link AwsMetricTransformerFactory}.
 */
public class PredefinedMetricTransformerTest {
    /**
     * This unit test case is used to ensure every service specific request
     * metric transformer is defined and made available during build time.
     * This test must not fail or else it means there is a bug somewhere in
     * setting up the service specific metric transformers and therefore
     * demands SDE investigation.
     */
    @Test
    public void ensureAWSSpecificTransformerDefined() throws Exception {
        for (AwsMetricTransformerFactory aws: AwsMetricTransformerFactory.values()) {
            String className = AwsMetricTransformerFactory
                .buildRequestMetricTransformerFqcn(
                    aws.name(),
                    AwsMetricTransformerFactory.DEFAULT_METRIC_TRANSFORM_PROVIDER_PACKAGE);
            Class<?> clazz = Class.forName(className);
            assertTrue(clazz.newInstance() instanceof RequestMetricTransformer);
        }
    }

    @Test
    public void testExceptionCounterMetricEnabled() {
        PredefinedMetricTransformer pmt = new PredefinedMetricTransformer();
        DefaultRequest<?> mockRequest = new DefaultRequest<Object>("TestServiceName");
        // Note the use of AwsRequestMetrics which means it's enabled
        AwsRequestMetrics mockRequestMetrics = new AwsRequestMetricsFullSupport();
        mockRequestMetrics.incrementCounter(Field.Exception);
        mockRequest.setAwsRequestMetrics(mockRequestMetrics);
        List<MetricDatum> list = pmt.counterMetricOf(Field.Exception, mockRequest, null, EXCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 1);
        MetricDatum datum = list.get(0);
        assertEquals("TestServiceName", datum.getMetricName());
        assertEquals(StandardUnit.Count.name(), datum.getUnit());
        assertTrue(datum.getValue() == 1.0);
        // Test count of 2 exceptions
        mockRequestMetrics.incrementCounter(Field.Exception);
        list = pmt.counterMetricOf(Field.Exception, mockRequest, null, EXCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 1);
        datum = list.get(0);
        assertTrue(datum.getValue() == 2.0);
    }

    @Test
    public void testThrottleExceptionCounterMetricEnabled() {
        PredefinedMetricTransformer pmt = new PredefinedMetricTransformer();
        DefaultRequest<?> mockRequest = new DefaultRequest<Object>("TestServiceName");
        // Note the use of AwsRequestMetrics which means it's enabled
        AwsRequestMetrics mockRequestMetrics = new AwsRequestMetricsFullSupport();
        mockRequestMetrics.incrementCounter(Field.ThrottleException);
        mockRequest.setAwsRequestMetrics(mockRequestMetrics);
        List<MetricDatum> list = pmt.counterMetricOf(Field.ThrottleException, mockRequest, null, EXCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 1);
        MetricDatum datum = list.get(0);
        assertEquals("TestServiceName", datum.getMetricName());
        assertEquals(StandardUnit.Count.name(), datum.getUnit());
        assertTrue(datum.getValue() == 1.0);
        // Test count of 2 exceptions
        mockRequestMetrics.incrementCounter(Field.ThrottleException);
        list = pmt.counterMetricOf(Field.ThrottleException, mockRequest, null, EXCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 1);
        datum = list.get(0);
        assertTrue(datum.getValue() == 2.0);
    }

    @Test
    public void testZeroExceptionCounterMetricEnabled() {
        PredefinedMetricTransformer pmt = new PredefinedMetricTransformer();
        DefaultRequest<?> mockRequest = new DefaultRequest<Object>("TestServiceName");
        AwsRequestMetrics mockRequestMetrics = new AwsRequestMetricsFullSupport();
        mockRequest.setAwsRequestMetrics(mockRequestMetrics);
        List<MetricDatum> list = pmt.counterMetricOf(Field.Exception, mockRequest, null, EXCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 0);
    }

    @Test
    public void testExceptionCounterMetricDisabled() {
        PredefinedMetricTransformer pmt = new PredefinedMetricTransformer();
        DefaultRequest<?> mockRequest = new DefaultRequest<Object>("TestServiceName");
        // Note the use of AwsRequestMetrics which means it's disabled
        AwsRequestMetrics mockRequestMetrics = new AwsRequestMetrics();
        mockRequestMetrics.incrementCounter(Field.Exception);
        mockRequest.setAwsRequestMetrics(mockRequestMetrics);
        List<MetricDatum> list = pmt.counterMetricOf(Field.Exception, mockRequest, null, EXCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 0);
    }

    @Test
    public void testExceptionMeticReturningTwoDatum() {
        PredefinedMetricTransformer pmt = new PredefinedMetricTransformer();
        DefaultRequest<?> mockRequest = new DefaultRequest<PutMetricDataRequest>(
                new PutMetricDataRequest(), "TestServiceName");
        // Note the use of AwsRequestMetrics which means it's enabled
        AwsRequestMetrics mockRequestMetrics = new AwsRequestMetricsFullSupport();
        mockRequestMetrics.incrementCounter(Field.Exception);
        mockRequest.setAwsRequestMetrics(mockRequestMetrics);
        List<MetricDatum> metricData = pmt.counterMetricOf(Field.Exception, mockRequest, null, INCLUDE_REQUEST_TYPE);
        assertTrue(metricData.size() == 2);
        final MetricDatum firstDatum = metricData.get(0);
        assertEquals("TestServiceName", firstDatum.getMetricName());
        assertEquals(StandardUnit.Count.name(), firstDatum.getUnit());
        assertTrue(firstDatum.getValue() == 1.0);
        // Check the first dimension
        int diff = DimensionComparator.INSTANCE.compare(
            new Dimension()
                .withName(Dimensions.MetricType.name())
                .withValue(Field.Exception.name()),
        firstDatum.getDimensions().get(0));
        assertTrue(diff == 0);
        // Check the second request specific datum
        final MetricDatum secondDatum = metricData.get(1);
        // Check the 1st dimension
        diff = DimensionComparator.INSTANCE.compare(
                new Dimension()
                    .withName(Dimensions.MetricType.name())
                    .withValue(Field.Exception.name()),
                    secondDatum.getDimensions().get(0));
        assertTrue(diff == 0);
        // Check the 2nd dimension
        diff = DimensionComparator.INSTANCE.compare(
                new Dimension()
                    .withName(Dimensions.RequestType.name())
                    .withValue(mockRequest.getOriginalRequest().getClass().getSimpleName()),
                    secondDatum.getDimensions().get(1));
        assertTrue(diff == 0);
        assertEquals(firstDatum.getMetricName(), secondDatum.getMetricName());
        assertEquals(firstDatum.getTimestamp(), secondDatum.getTimestamp());
        assertEquals(firstDatum.getUnit(), secondDatum.getUnit());
        assertEquals(firstDatum.getValue(), secondDatum.getValue());
        assertEquals(firstDatum.getStatisticValues(), secondDatum.getStatisticValues());
        assertTrue(firstDatum.getDimensions().size() < secondDatum.getDimensions().size());
    }

    @Test
    public void testLatencyMetricEnabled() throws Exception {
        PredefinedMetricTransformer pmt = new PredefinedMetricTransformer();
        DefaultRequest<?> mockRequest = new DefaultRequest<PutMetricDataRequest>(
            new PutMetricDataRequest(), "TestServiceName");
        // Note the use of AwsRequestMetrics which means it's enabled
        AwsRequestMetrics mockRequestMetrics = new AwsRequestMetricsFullSupport();
        mockRequestMetrics.startEvent(Field.HttpRequestTime);
        Thread.sleep(100);
        mockRequestMetrics.endEvent(Field.HttpRequestTime);
        mockRequest.setAwsRequestMetrics(mockRequestMetrics);
        List<MetricDatum> list = pmt.latencyMetricOf(Field.HttpRequestTime, mockRequest, null, INCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 1);
        MetricDatum datum = list.get(0);
        assertEquals("TestServiceName", datum.getMetricName());
        assertEquals(StandardUnit.Milliseconds.name(), datum.getUnit());
        assertTrue(datum.getValue() >= 100.0);
        List<Dimension> dims = datum.getDimensions();
        assertTrue(dims.size() == 2);
        // exclude request dimension
        list = pmt.latencyMetricOf(Field.HttpRequestTime, mockRequest, null, EXCLUDE_REQUEST_TYPE);
        assertTrue(list.size() == 1);
        datum = list.get(0);
        assertEquals("TestServiceName", datum.getMetricName());
        assertEquals(StandardUnit.Milliseconds.name(), datum.getUnit());
        assertTrue(datum.getValue() >= 100.0);
        dims = datum.getDimensions();
        assertTrue(dims.size() == 1);
    }

    @Test
    public void latencyOfClientExecuteTime() throws Exception {
        PredefinedMetricTransformer pmt = new PredefinedMetricTransformer();
        DefaultRequest<?> mockRequest = new DefaultRequest<PutMetricDataRequest>(
            new PutMetricDataRequest(), "TestServiceName");
        // Note the use of AwsRequestMetrics which means it's enabled
        AwsRequestMetrics mockRequestMetrics = new AwsRequestMetricsFullSupport();
        final long startNano = mockRequestMetrics.getTimingInfo().getStartTimeNano();
        Thread.sleep(100);
        mockRequestMetrics.getTimingInfo().endTiming();
        final long endNano = mockRequestMetrics.getTimingInfo().getEndTimeNano();
        final double expectedDuration = TimingInfo.durationMilliOf(startNano, endNano);
        mockRequest.setAwsRequestMetrics(mockRequestMetrics);
        List<MetricDatum> list = pmt.latencyOfClientExecuteTime(mockRequest, null);
        assertTrue(list.size() == 1);
        MetricDatum datum = list.get(0);
        assertEquals("TestServiceName", datum.getMetricName());
        assertEquals(StandardUnit.Milliseconds.name(), datum.getUnit());
        assertTrue(datum.getValue() == expectedDuration);
        List<Dimension> dims = datum.getDimensions();
        assertTrue(dims.size() == 2);
        System.err.println(dims);
    }
}
