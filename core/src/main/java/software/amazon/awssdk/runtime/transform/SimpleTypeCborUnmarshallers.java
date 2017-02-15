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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;

@SdkInternalApi
public class SimpleTypeCborUnmarshallers {
    /**
     * Unmarshaller for String values.
     */
    public static class StringCborUnmarshaller implements Unmarshaller<String, JsonUnmarshallerContext> {
        private static final StringCborUnmarshaller instance = new StringCborUnmarshaller();

        public static StringCborUnmarshaller getInstance() {
            return instance;
        }

        public String unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.readText();
        }
    }

    /**
     * Unmarshaller for Double values.
     */
    public static class DoubleCborUnmarshaller implements Unmarshaller<Double, JsonUnmarshallerContext> {
        private static final DoubleCborUnmarshaller instance = new DoubleCborUnmarshaller();

        public static DoubleCborUnmarshaller getInstance() {
            return instance;
        }

        public Double unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getDoubleValue();
        }
    }

    /**
     * Unmarshaller for Integer values.
     */
    public static class IntegerCborUnmarshaller implements Unmarshaller<Integer, JsonUnmarshallerContext> {
        private static final IntegerCborUnmarshaller instance = new IntegerCborUnmarshaller();

        public static IntegerCborUnmarshaller getInstance() {
            return instance;
        }

        public Integer unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getIntValue();
        }
    }

    public static class BigIntegerCborUnmarshaller implements Unmarshaller<BigInteger, JsonUnmarshallerContext> {
        private static final BigIntegerCborUnmarshaller instance = new BigIntegerCborUnmarshaller();

        public static BigIntegerCborUnmarshaller getInstance() {
            return instance;
        }

        public BigInteger unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            JsonParser parser = unmarshallerContext.getJsonParser();
            JsonToken current = parser.getCurrentToken();
            if (current == JsonToken.VALUE_NUMBER_INT) {
                return parser.getBigIntegerValue();
            } else if (current == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object embedded = parser.getEmbeddedObject();
                return new BigInteger((byte[]) embedded);
            } else {
                throw new SdkClientException("Invalid BigInteger Format.");
            }
        }
    }

    public static class BigDecimalCborUnmarshaller implements Unmarshaller<BigDecimal, JsonUnmarshallerContext> {
        private static final BigDecimalCborUnmarshaller instance = new BigDecimalCborUnmarshaller();

        public static BigDecimalCborUnmarshaller getInstance() {
            return instance;
        }

        public BigDecimal unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            JsonParser parser = unmarshallerContext.getJsonParser();
            Unmarshaller<BigInteger, JsonUnmarshallerContext> bigIntegerUnmarshaller = unmarshallerContext.getUnmarshaller(BigInteger.class);

            JsonToken current = parser.getCurrentToken();
            if (current != JsonToken.START_ARRAY) {
                throw new SdkClientException("Invalid BigDecimal Format.");
            }
            parser.nextToken();
            int exponent = parser.getIntValue();
            parser.nextToken();
            BigInteger mantissa = bigIntegerUnmarshaller.unmarshall(unmarshallerContext);
            return new BigDecimal(mantissa, exponent);
        }
    }

    /**
     * Unmarshaller for Boolean values.
     */
    public static class BooleanCborUnmarshaller implements Unmarshaller<Boolean, JsonUnmarshallerContext> {
        private static final BooleanCborUnmarshaller instance = new BooleanCborUnmarshaller();

        public static BooleanCborUnmarshaller getInstance() {
            return instance;
        }

        public Boolean unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getBooleanValue();
        }
    }

    /**
     * Unmarshaller for Float values.
     */
    public static class FloatCborUnmarshaller implements Unmarshaller<Float, JsonUnmarshallerContext> {
        private static final FloatCborUnmarshaller instance = new FloatCborUnmarshaller();

        public static FloatCborUnmarshaller getInstance() {
            return instance;
        }

        public Float unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getFloatValue();
        }
    }

    /**
     * Unmarshaller for Long values.
     */
    public static class LongCborUnmarshaller implements Unmarshaller<Long, JsonUnmarshallerContext> {
        private static final LongCborUnmarshaller instance = new LongCborUnmarshaller();

        public static LongCborUnmarshaller getInstance() {
            return instance;
        }

        public Long unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getLongValue();
        }
    }

    /**
     * Unmarshaller for Byte values.
     */
    public static class ByteCborUnmarshaller implements Unmarshaller<Byte, JsonUnmarshallerContext> {
        private static final ByteCborUnmarshaller instance = new ByteCborUnmarshaller();

        public static ByteCborUnmarshaller getInstance() {
            return instance;
        }

        public Byte unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getByteValue();
        }
    }

    /**
     * Unmarshaller for Date values - JSON dates come in as epoch seconds.
     */
    public static class DateCborUnmarshaller implements Unmarshaller<Date, JsonUnmarshallerContext> {
        private static final DateCborUnmarshaller instance = new DateCborUnmarshaller();

        public static DateCborUnmarshaller getInstance() {
            return instance;
        }

        public Date unmarshall(JsonUnmarshallerContext unmarshallerContext)
                throws Exception {
            return new Date(unmarshallerContext.getJsonParser().getLongValue());
        }
    }

    /**
     * Unmarshaller for ByteBuffer values.
     */
    public static class ByteBufferCborUnmarshaller implements Unmarshaller<ByteBuffer, JsonUnmarshallerContext> {
        private static final ByteBufferCborUnmarshaller instance = new ByteBufferCborUnmarshaller();

        public static ByteBufferCborUnmarshaller getInstance() {
            return instance;
        }

        public ByteBuffer unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return ByteBuffer.wrap(unmarshallerContext.getJsonParser().getBinaryValue());

        }
    }

    /**
     * Unmarshaller for Short values.
     */
    public static class ShortCborUnmarshaller implements Unmarshaller<Short, JsonUnmarshallerContext> {
        private static final ShortCborUnmarshaller instance = new ShortCborUnmarshaller();

        public static ShortCborUnmarshaller getInstance() {
            return instance;
        }

        public Short unmarshall(JsonUnmarshallerContext unmarshallerContext) throws Exception {
            return unmarshallerContext.getJsonParser().getShortValue();
        }
    }
}