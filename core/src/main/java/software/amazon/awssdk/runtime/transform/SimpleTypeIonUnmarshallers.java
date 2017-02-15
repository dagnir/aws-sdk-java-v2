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
import software.amazon.awssdk.annotation.SdkInternalApi;

@SdkInternalApi
public class SimpleTypeIonUnmarshallers {
    public static class StringIonUnmarshaller implements Unmarshaller<String, JsonUnmarshallerContext> {
        private static final StringIonUnmarshaller instance = new StringIonUnmarshaller();

        public static StringIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public String unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.readText();
        }
    }


    public static class DoubleIonUnmarshaller implements Unmarshaller<Double, JsonUnmarshallerContext> {
        private static final DoubleIonUnmarshaller instance = new DoubleIonUnmarshaller();

        public static DoubleIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Double unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getDoubleValue();
        }
    }

    public static class IntegerIonUnmarshaller implements Unmarshaller<Integer, JsonUnmarshallerContext> {
        private static final IntegerIonUnmarshaller instance = new IntegerIonUnmarshaller();

        public static IntegerIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Integer unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getIntValue();
        }
    }

    public static class BigIntegerIonUnmarshaller implements Unmarshaller<BigInteger, JsonUnmarshallerContext> {
        private static final BigIntegerIonUnmarshaller instance = new BigIntegerIonUnmarshaller();

        public static BigIntegerIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public BigInteger unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getBigIntegerValue();
        }
    }

    public static class BigDecimalIonUnmarshaller implements Unmarshaller<BigDecimal, JsonUnmarshallerContext> {
        private static final BigDecimalIonUnmarshaller instance = new BigDecimalIonUnmarshaller();

        public static BigDecimalIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public BigDecimal unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getDecimalValue();
        }
    }

    public static class BooleanIonUnmarshaller implements Unmarshaller<Boolean, JsonUnmarshallerContext> {
        private static final BooleanIonUnmarshaller instance = new BooleanIonUnmarshaller();

        public static BooleanIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Boolean unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getBooleanValue();
        }
    }

    public static class FloatIonUnmarshaller implements Unmarshaller<Float, JsonUnmarshallerContext> {
        private static final FloatIonUnmarshaller instance = new FloatIonUnmarshaller();

        public static FloatIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Float unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getFloatValue();
        }
    }

    public static class LongIonUnmarshaller implements Unmarshaller<Long, JsonUnmarshallerContext> {
        private static final LongIonUnmarshaller instance = new LongIonUnmarshaller();

        public static LongIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Long unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getLongValue();
        }
    }

    public static class ByteIonUnmarshaller implements Unmarshaller<Byte, JsonUnmarshallerContext> {
        private static final ByteIonUnmarshaller instance = new ByteIonUnmarshaller();

        public static ByteIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Byte unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getByteValue();
        }
    }

    public static class DateIonUnmarshaller implements Unmarshaller<Date, JsonUnmarshallerContext> {
        private static final DateIonUnmarshaller instance = new DateIonUnmarshaller();

        public static DateIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Date unmarshall(JsonUnmarshallerContext context) throws Exception {
            return (Date) context.getJsonParser().getEmbeddedObject();
        }
    }

    public static class ByteBufferIonUnmarshaller implements Unmarshaller<ByteBuffer, JsonUnmarshallerContext> {
        private static final ByteBufferIonUnmarshaller instance = new ByteBufferIonUnmarshaller();

        public static ByteBufferIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public ByteBuffer unmarshall(JsonUnmarshallerContext context) throws Exception {
            return (ByteBuffer) context.getJsonParser().getEmbeddedObject();
        }
    }

    public static class ShortIonUnmarshaller implements Unmarshaller<Short, JsonUnmarshallerContext> {
        private static final ShortIonUnmarshaller instance = new ShortIonUnmarshaller();

        public static ShortIonUnmarshaller getInstance() {
            return instance;
        }

        @Override
        public Short unmarshall(JsonUnmarshallerContext context) throws Exception {
            return context.getJsonParser().getShortValue();
        }
    }
}
