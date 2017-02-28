package software.amazon.awssdk.metrics.internal.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;

import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

public class BlockingRequestBuilderTest {

    @Test
    public void cloneMetricDatum() {
        BlockingRequestBuilder b = new BlockingRequestBuilder(new CloudWatchMetricConfig(), null);
        Collection<Dimension> dimensions = Collections.emptyList();
        MetricDatum md = new MetricDatum().withDimensions(dimensions);
        assertNotSame("Expect a new collection to be created", md.getDimensions(), dimensions);
        assertTrue(0 == md.getDimensions().size());
        md.withDimensions(new Dimension().withName("Name1").withValue("Value1"));
        assertTrue(1 == md.getDimensions().size());
        md.withDimensions(new Dimension().withName("Name2").withValue("Value2"));
        assertTrue(2 == md.getDimensions().size());
        md.withMetricName("MetricName");
        md.withStatisticValues(new StatisticSet().withMaximum(100.0)
                .withMinimum(10.0).withSampleCount(12.34).withSum(99.9));
        md.withTimestamp(new Date());
        md.withUnit(StandardUnit.Milliseconds);
        md.withValue(56.78);

        MetricDatum md2 = b.cloneMetricDatum(md);

        assertNotSame(md.getDimensions(), md2.getDimensions());
        assertTrue(Arrays.equals(md.getDimensions().toArray(), md2.getDimensions().toArray()));
        assertEquals(md.getMetricName(), md2.getMetricName());
        assertEquals(md.getStatisticValues(), md2.getStatisticValues());
        assertEquals(md.getTimestamp(), md2.getTimestamp());
        assertEquals(md.getUnit(), md2.getUnit());
        assertEquals(md.getValue(), md2.getValue());
    }

}
