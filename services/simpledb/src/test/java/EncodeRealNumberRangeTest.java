import org.junit.Assert;
import org.junit.Test;

import software.amazon.awssdk.services.simpledb.util.SimpleDBUtils;

public class EncodeRealNumberRangeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeTooLargeNumber() throws Exception {
        testEncodeNumber(6060450912.0f, "6045091200");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeTooSmallNumber() throws Exception {
        testEncodeNumber(-1.0f, "6045091200");
    }

    @Test
    public void testEncodeLargeNumbers() throws Exception {
        testEncodeNumber(60450912.0f, "6045091200");
        testEncodeNumber(21735100.0f, "2173510000");
        testEncodeNumber(31200796.0f, "3120079600");
        testEncodeNumber(18908200.0f, "1890820000");
        testEncodeNumber(15487574.0f, "1548757400");

        testEncodeNumber0(60450912.0f, "60450912");
        testEncodeNumber0(21735100.0f, "21735100");
        testEncodeNumber0(31200796.0f, "31200796");
        testEncodeNumber0(18908200.0f, "18908200");
        testEncodeNumber0(15487574.0f, "15487574");
    }

    private void testEncodeNumber(float value, String expectedValue) throws Exception {
        int numDigitsLeft = 8;
        int numDigitsRight = 2;
        int offset = 0;

        String mungedValue = SimpleDBUtils.encodeRealNumberRange(value, numDigitsLeft, numDigitsRight, offset);

        Assert.assertEquals(expectedValue, mungedValue);
    }

    private void testEncodeNumber0(float value, String expectedValue) throws Exception {
        int numDigitsLeft = 8;
        int numDigitsRight = 0;
        int offset = 0;

        String mungedValue = SimpleDBUtils.encodeRealNumberRange(value, numDigitsLeft, numDigitsRight, offset);

        Assert.assertEquals(expectedValue, mungedValue);
    }

}
