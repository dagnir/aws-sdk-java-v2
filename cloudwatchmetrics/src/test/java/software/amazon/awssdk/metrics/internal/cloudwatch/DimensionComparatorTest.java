package software.amazon.awssdk.metrics.internal.cloudwatch;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.test.util.IndexValues;

public class DimensionComparatorTest {
    // Test all possible combinations of the values of two test dimensions
    @Test
    public void combinations() {
        final boolean DEBUG = false;
        String[] names = {null, "dim1", "dim2"};    // 3 possible values
        String[] value = {null, "val1", "val2", "val2x"};   // 4 possible values
        // Note there are 4*3=12 possible values in building a test dimension.
        // To compare two test dimensions, it therefore forms a 12 by 12 matrix
        // for all the possible combinations.
        int ones=0, minus_ones=0, zeros=0;  // used to count the different results.
        // IndexValues is used to generate all possible combinations of the
        // above names and values in two Dimension objects
        for (int[] is : new IndexValues(names.length, value.length, names.length, value.length)) {
            int j=0;
            Dimension from = new Dimension().withName(names[is[j++]]).withValue(value[is[j++]]);
            Dimension to = new Dimension().withName(names[is[j++]]).withValue(value[is[j++]]);
            int compared = DimensionComparator.INSTANCE.compare(from, to);
            if (DEBUG)
                System.out.println("from=" + from + ", to=" + to + ", compared=" + compared);
            switch (compared) {
                case -1: minus_ones++; break;
                case 0: zeros++; break;
                case 1: ones++; break;
                default:
                    throw new IllegalStateException("Unexpected comparision result!");
            }
        }
        if (DEBUG)
            System.out.println("1's=" + ones + ", 0's=" + zeros + ", -1's = " + minus_ones);
        assertTrue(ones == minus_ones); // The matrix is symmetric
        assertTrue(zeros == 12);    // exactly 12 diagonal comparisons
        assertTrue(12*12 == ones+minus_ones+zeros); // total test cases is exactly 12^2
    }

    @Test
    public void test() {
        // Test case entry format: from, to, expected-result
        Object[][] cases =  
        { 
            {   new Dimension().withName("dim1").withValue("val1"),
                new Dimension().withName("dim1").withValue("val1"), 
                0
            },
            {   new Dimension().withName("dim2").withValue("val2"),
                new Dimension().withName("dim2").withValue("val2x"),
                -1
            },
            {   new Dimension().withName("dim2").withValue("val2x"),
                new Dimension().withName("dim2").withValue("val2"),
                1
            },
            {   new Dimension().withName("dim3").withValue("val2x"),
                new Dimension().withName("dim2").withValue("val2x"),
                1
            },
            {   new Dimension().withName(null).withValue("val2x"),
                new Dimension().withName("dim2").withValue("val2x"),
                -1
            },
            {   new Dimension().withName(null).withValue("val2x"),
                new Dimension().withName(null).withValue("val2x"),
                0
            },
            {   new Dimension().withName(null).withValue(null),
                new Dimension().withName(null).withValue("val2x"),
                -1
            },
            {   new Dimension().withName(null).withValue(null),
                new Dimension().withName(null).withValue(null),
                0
            },
        };
        int i=0;
        for (Object[] entry: cases) {
            int j=0;
            Dimension from = (Dimension) entry[j++];
            Dimension to = (Dimension) entry[j++];
            int expected = (Integer) entry[j++];
            int actual = DimensionComparator.INSTANCE.compare(from, to);
            assertTrue("i=" + i++ + ", actual=" + actual + ", expect=" + expected, actual == expected);
        }
    }
}
