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
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.util.Base64;
import software.amazon.awssdk.util.DateUtils;

@SdkInternalApi
public class SimpleTypeJsonUnmarshallers {
    /**
     * Unmarshaller for String values.
     */
    public static class StringJsonUnmarshaller implements Unmarshaller<String, JsonUnmarshallerContext> {
        private static final StringJsonUnmarshaller instance = new StringJsonUnmarshaller();

        public static StringJsonUnmarshaller getInstance() {
            return instance;
        }

        public String unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.readText();
        }
    }

    /**
     * Unmarshaller for Double values.
     */
    public static class DoubleJsonUnmarshaller implements Unmarshaller<Double, JsonUnmarshallerContext> {
        private static final DoubleJsonUnmarshaller instance = new DoubleJsonUnmarshaller();

        public static DoubleJsonUnmarshaller getInstance() {
            return instance;
        }

        public Double unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String doubleString = unmarshallerContext.readText();
            return (doubleString == null) ? null : Double.parseDouble(doubleString);
        }
    }

    /**
     * Unmarshaller for Integer values.
     */
    public static class IntegerJsonUnmarshaller implements Unmarshaller<Integer, JsonUnmarshallerContext> {
        private static final IntegerJsonUnmarshaller instance = new IntegerJsonUnmarshaller();

        public static IntegerJsonUnmarshaller getInstance() {
            return instance;
        }

        public Integer unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String intString = unmarshallerContext.readText();
            return (intString == null) ? null : Integer.parseInt(intString);
        }
    }

    public static class BigIntegerJsonUnmarshaller implements Unmarshaller<BigInteger, JsonUnmarshallerContext> {
        private static final BigIntegerJsonUnmarshaller instance = new BigIntegerJsonUnmarshaller();

        public static BigIntegerJsonUnmarshaller getInstance() {
            return instance;
        }

        public BigInteger unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String intString = unmarshallerContext.readText();
            return (intString == null) ? null : new BigInteger(intString);
        }
    }

    public static class BigDecimalJsonUnmarshaller implements Unmarshaller<BigDecimal, JsonUnmarshallerContext> {
        private static final BigDecimalJsonUnmarshaller instance = new BigDecimalJsonUnmarshaller();

        public static BigDecimalJsonUnmarshaller getInstance() {
            return instance;
        }

        public BigDecimal unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String s = unmarshallerContext.readText();
            return (s == null) ? null : new BigDecimal(s);
        }
    }

    /**
     * Unmarshaller for Boolean values.
     */
    public static class BooleanJsonUnmarshaller implements Unmarshaller<Boolean, JsonUnmarshallerContext> {
        private static final BooleanJsonUnmarshaller instance = new BooleanJsonUnmarshaller();

        public static BooleanJsonUnmarshaller getInstance() {
            return instance;
        }

        public Boolean unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String booleanString = unmarshallerContext.readText();
            return (booleanString == null) ? null : Boolean.parseBoolean(booleanString);
        }
    }

    /**
     * Unmarshaller for Float values.
     */
    public static class FloatJsonUnmarshaller implements Unmarshaller<Float, JsonUnmarshallerContext> {
        private static final FloatJsonUnmarshaller instance = new FloatJsonUnmarshaller();

        public static FloatJsonUnmarshaller getInstance() {
            return instance;
        }

        public Float unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String floatString = unmarshallerContext.readText();
            return (floatString == null) ? null : Float.valueOf(floatString);
        }
    }

    /**
     * Unmarshaller for Long values.
     */
    public static class LongJsonUnmarshaller implements Unmarshaller<Long, JsonUnmarshallerContext> {
        private static final LongJsonUnmarshaller instance = new LongJsonUnmarshaller();

        public static LongJsonUnmarshaller getInstance() {
            return instance;
        }

        public Long unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String longString = unmarshallerContext.readText();
            return (longString == null) ? null : Long.parseLong(longString);
        }
    }

    /**
     * Unmarshaller for Byte values.
     */
    public static class ByteJsonUnmarshaller implements Unmarshaller<Byte, JsonUnmarshallerContext> {
        private static final ByteJsonUnmarshaller instance = new ByteJsonUnmarshaller();

        public static ByteJsonUnmarshaller getInstance() {
            return instance;
        }

        public Byte unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String byteString = unmarshallerContext.readText();
            return (byteString == null) ? null : Byte.valueOf(byteString);
        }
    }

    /**
     * Unmarshaller for Date values - JSON dates come in as epoch seconds.
     */
    public static class DateJsonUnmarshaller implements Unmarshaller<Date, JsonUnmarshallerContext> {
        private static final DateJsonUnmarshaller instance = new DateJsonUnmarshaller();

        public static DateJsonUnmarshaller getInstance() {
            return instance;
        }

        public Date unmarshall(JsonUnmarshallerContext unmarshallerContext)
                throws Exception {
            return DateUtils.parseServiceSpecificDate(unmarshallerContext
                                                              .readText());
        }
    }

    /**
     * Unmarshaller for ByteBuffer values.
     */
    public static class ByteBufferJsonUnmarshaller implements Unmarshaller<ByteBuffer, JsonUnmarshallerContext> {
        private static final ByteBufferJsonUnmarshaller instance = new ByteBufferJsonUnmarshaller();

        public static ByteBufferJsonUnmarshaller getInstance() {
            return instance;
        }

        public ByteBuffer unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String base64EncodedString = unmarshallerContext.readText();
            if (base64EncodedString == null) {
                return null;
            }
            byte[] decodedBytes = Base64.decode(base64EncodedString);
            return ByteBuffer.wrap(decodedBytes);

        }
    }

    /**
     * Unmarshaller for Character values.
     */
    public static class CharacterJsonUnmarshaller implements Unmarshaller<Character, JsonUnmarshallerContext> {
        private static final CharacterJsonUnmarshaller instance = new CharacterJsonUnmarshaller();

        public static CharacterJsonUnmarshaller getInstance() {
            return instance;
        }

        public Character unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
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
    public static class ShortJsonUnmarshaller implements Unmarshaller<Short, JsonUnmarshallerContext> {
        private static final ShortJsonUnmarshaller instance = new ShortJsonUnmarshaller();

        public static ShortJsonUnmarshaller getInstance() {
            return instance;
        }

        public Short unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            String shortString = unmarshallerContext.readText();
            return (shortString == null) ? null : Short.valueOf(shortString);
        }
    }
}
