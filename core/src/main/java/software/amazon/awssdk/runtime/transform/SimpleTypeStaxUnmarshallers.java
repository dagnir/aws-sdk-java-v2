/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.runtime.transform;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.util.Base64;
import software.amazon.awssdk.util.DateUtils;

/**
 * Collection of StAX unmarshallers for simple data types.
 */
public class SimpleTypeStaxUnmarshallers {
    /** Shared logger. */
    private static Log log = LogFactory.getLog(SimpleTypeStaxUnmarshallers.class);

    /**
     * Unmarshaller for String values.
     */
    public static class StringStaxUnmarshaller implements Unmarshaller<String, StaxUnmarshallerContext> {
        private static final StringStaxUnmarshaller INSTANCE = new StringStaxUnmarshaller();

        public static StringStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public String unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.readText();
        }
    }

    public static class BigDecimalStaxUnmarshaller implements Unmarshaller<BigDecimal, StaxUnmarshallerContext> {
        private static final BigDecimalStaxUnmarshaller INSTANCE = new BigDecimalStaxUnmarshaller();

        public static BigDecimalStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public BigDecimal unmarshall(StaxUnmarshallerContext unmarshallerContext)
                throws Exception {
            String s = unmarshallerContext.readText();
            return (s == null) ? null : new BigDecimal(s);
        }
    }

    public static class BigIntegerStaxUnmarshaller implements Unmarshaller<BigInteger, StaxUnmarshallerContext> {
        private static final BigIntegerStaxUnmarshaller INSTANCE = new BigIntegerStaxUnmarshaller();

        public static BigIntegerStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public BigInteger unmarshall(StaxUnmarshallerContext unmarshallerContext)
                throws Exception {
            String s = unmarshallerContext.readText();
            return (s == null) ? null : new BigInteger(s);
        }
    }

    /**
     * Unmarshaller for Double values.
     */
    public static class DoubleStaxUnmarshaller implements Unmarshaller<Double, StaxUnmarshallerContext> {
        private static final DoubleStaxUnmarshaller INSTANCE = new DoubleStaxUnmarshaller();

        public static DoubleStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Double unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String doubleString = unmarshallerContext.readText();
            return (doubleString == null) ? null : Double.parseDouble(doubleString);
        }
    }

    /**
     * Unmarshaller for Integer values.
     */
    public static class IntegerStaxUnmarshaller implements Unmarshaller<Integer, StaxUnmarshallerContext> {
        private static final IntegerStaxUnmarshaller INSTANCE = new IntegerStaxUnmarshaller();

        public static IntegerStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Integer unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String intString = unmarshallerContext.readText();
            return (intString == null) ? null : Integer.parseInt(intString);
        }
    }

    /**
     * Unmarshaller for Boolean values.
     */
    public static class BooleanStaxUnmarshaller implements Unmarshaller<Boolean, StaxUnmarshallerContext> {
        private static final BooleanStaxUnmarshaller INSTANCE = new BooleanStaxUnmarshaller();

        public static BooleanStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Boolean unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String booleanString = unmarshallerContext.readText();
            return (booleanString == null) ? null : Boolean.parseBoolean(booleanString);
        }
    }

    /**
     * Unmarshaller for Float values.
     */
    public static class FloatStaxUnmarshaller implements Unmarshaller<Float, StaxUnmarshallerContext> {
        private static final FloatStaxUnmarshaller INSTANCE = new FloatStaxUnmarshaller();

        public static FloatStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Float unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String floatString = unmarshallerContext.readText();
            return (floatString == null) ? null : Float.valueOf(floatString);
        }
    }

    /**
     * Unmarshaller for Long values.
     */
    public static class LongStaxUnmarshaller implements Unmarshaller<Long, StaxUnmarshallerContext> {
        private static final LongStaxUnmarshaller INSTANCE = new LongStaxUnmarshaller();

        public static LongStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Long unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String longString = unmarshallerContext.readText();
            return (longString == null) ? null : Long.parseLong(longString);
        }
    }

    /**
     * Unmarshaller for Byte values.
     */
    public static class ByteStaxUnmarshaller implements Unmarshaller<Byte, StaxUnmarshallerContext> {
        private static final ByteStaxUnmarshaller INSTANCE = new ByteStaxUnmarshaller();

        public static ByteStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Byte unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String byteString = unmarshallerContext.readText();
            return (byteString == null) ? null : Byte.valueOf(byteString);
        }
    }

    /**
     * Unmarshaller for Date values.
     */
    public static class DateStaxUnmarshaller implements Unmarshaller<Date, StaxUnmarshallerContext> {
        private static final DateStaxUnmarshaller INSTANCE = new DateStaxUnmarshaller();

        public static DateStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Date unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String dateString = unmarshallerContext.readText();
            if (dateString == null) {
                return null;
            }

            try {
                return DateUtils.parseIso8601Date(dateString);
            } catch (Exception e) {
                log.warn("Unable to parse date '" + dateString + "':  " + e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     * Unmarshaller for ByteBuffer values.
     */
    public static class ByteBufferStaxUnmarshaller implements Unmarshaller<ByteBuffer, StaxUnmarshallerContext> {
        private static final ByteBufferStaxUnmarshaller INSTANCE = new ByteBufferStaxUnmarshaller();

        public static ByteBufferStaxUnmarshaller getInstance() {
            return INSTANCE;
        }

        public ByteBuffer unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String base64EncodedString = unmarshallerContext.readText();
            byte[] decodedBytes = Base64.decode(base64EncodedString);
            return ByteBuffer.wrap(decodedBytes);

        }
    }

    /**
     * Unmarshaller for Character values.
     */
    public static class CharacterJsonUnmarshaller implements Unmarshaller<Character, StaxUnmarshallerContext> {
        private static final CharacterJsonUnmarshaller INSTANCE = new CharacterJsonUnmarshaller();

        public static CharacterJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Character unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String charString = unmarshallerContext.readText();

            if (charString == null) {
                return null;
            }

            charString = charString.trim();
            if (charString.isEmpty() || charString.length() > 1) {
                throw new SdkClientException("'" + charString
                                             + "' cannot be converted to Character");
            }
            return Character.valueOf(charString.charAt(0));
        }
    }

    /**
     * Unmarshaller for Short values.
     */
    public static class ShortJsonUnmarshaller implements Unmarshaller<Short, StaxUnmarshallerContext> {
        private static final ShortJsonUnmarshaller INSTANCE = new ShortJsonUnmarshaller();

        public static ShortJsonUnmarshaller getInstance() {
            return INSTANCE;
        }

        public Short unmarshall(StaxUnmarshallerContext unmarshallerContext) throws Exception {
            String shortString = unmarshallerContext.readText();
            return (shortString == null) ? null : Short.valueOf(shortString);
        }
    }
}
