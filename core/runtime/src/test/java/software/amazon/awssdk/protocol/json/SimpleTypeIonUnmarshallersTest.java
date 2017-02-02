/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License is
 * located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.awssdk.protocol.json;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

import software.amazon.awssdk.protocol.json.IonFactory;
import software.amazon.awssdk.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.transform.JsonUnmarshallerContextImpl;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.BigDecimalIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.BigIntegerIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.BooleanIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.ByteBufferIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.ByteIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.DateIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.DoubleIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.FloatIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.IntegerIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.LongIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.ShortIonUnmarshaller;
import software.amazon.awssdk.transform.SimpleTypeIonUnmarshallers.StringIonUnmarshaller;
import software.amazon.awssdk.util.StringInputStream;
import com.fasterxml.jackson.core.JsonParser;

import software.amazon.ion.system.IonSystemBuilder;

public class SimpleTypeIonUnmarshallersTest {
    @Test
    public void unmarshalString() throws Exception {
        assertEquals("foo", StringIonUnmarshaller.getInstance().unmarshall(context("\"foo\"")));
    }

    @Test
    public void unmarshalDouble() throws Exception {
        assertEquals(new Double(123.456), DoubleIonUnmarshaller.getInstance().unmarshall(context("123.456")));
    }

    @Test
    public void unmarshalInteger() throws Exception {
        assertEquals(new Integer(-999), IntegerIonUnmarshaller.getInstance().unmarshall(context("-999")));
    }

    @Test
    public void unmarshalBigInteger() throws Exception {
        assertEquals(
                new BigInteger("123456789012345678901234567890"),
                BigIntegerIonUnmarshaller.getInstance().unmarshall(context("123456789012345678901234567890")));
    }

    @Test
    public void unmarshalBigDecimal() throws Exception {
        assertEquals(
                new BigDecimal("1234567890.12345678901234567890"),
                BigDecimalIonUnmarshaller.getInstance().unmarshall(context("1234567890.12345678901234567890")));
    }

    @Test
    public void unmarshalBoolean() throws Exception {
        assertEquals(Boolean.TRUE, BooleanIonUnmarshaller.getInstance().unmarshall(context("true")));
    }

    @Test
    public void unmarshalFloat() throws Exception {
        assertEquals(new Float(123.456f), FloatIonUnmarshaller.getInstance().unmarshall(context("123.456")));
    }

    @Test
    public void unmarshalLong() throws Exception {
        assertEquals(new Long(123456L), LongIonUnmarshaller.getInstance().unmarshall(context("123456")));
    }

    @Test
    public void unmarshalByte() throws Exception {
        assertEquals(new Byte((byte) 123), ByteIonUnmarshaller.getInstance().unmarshall(context("123")));
    }

    @Test
    public void unmarshalDate() throws Exception {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2000, 0, 1); // Month is zero-based
        assertEquals(calendar.getTime(), DateIonUnmarshaller.getInstance().unmarshall(context("2000-01-01T")));
    }

    @Test
    public void unmarshalByteBuffer() throws Exception {
        byte[] buffer = new byte[] {1, 2, 3, 4, 5, 6};
        assertEquals(ByteBuffer.wrap(buffer), ByteBufferIonUnmarshaller.getInstance().unmarshall(context("{{AQIDBAUG}}")));
    }

    @Test
    public void unmarshalShort() throws Exception {
        assertEquals(new Short((short) 1234), ShortIonUnmarshaller.getInstance().unmarshall(context("1234")));
    }

    private static JsonUnmarshallerContext context(String ion) throws Exception {
        JsonParser parser = new IonFactory(IonSystemBuilder.standard().build()).createParser(new StringInputStream(ion));
        JsonUnmarshallerContext context = new JsonUnmarshallerContextImpl(parser, null, null);
        context.nextToken();
        return context;
    }
}
