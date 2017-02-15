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

package software.amazon.awssdk.services.dynamodbv2.mapper;

import java.util.Calendar;
import java.util.Date;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBTable;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedTimestamp;
import software.amazon.awssdk.services.dynamodbv2.pojos.AutoKeyAndVal;

/**
 * Tests updating component attribute fields correctly.
 */
public class TypeConvertedTimestampIntegrationTest extends AbstractKeyAndValIntegrationTestCase {

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testCalendarTimestamp() throws Exception {
        final KeyAndCalendarTimestamp object = new KeyAndCalendarTimestamp();
        object.setVal(Calendar.getInstance());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testCalendarTimestampNull() {
        final KeyAndCalendarTimestamp object = new KeyAndCalendarTimestamp();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testDateTimestamp() throws Exception {
        final KeyAndDateTimestamp object = new KeyAndDateTimestamp();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testDateTimestampNull() {
        final KeyAndDateTimestamp object = new KeyAndDateTimestamp();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testLongTimestamp() throws Exception {
        final KeyAndLongTimestamp object = new KeyAndLongTimestamp();
        object.setVal(Calendar.getInstance().getTime().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testLongTimestampNull() {
        final KeyAndLongTimestamp object = new KeyAndLongTimestamp();
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testEstCalendarTimestamp() throws Exception {
        final KeyAndEstCalendarTimestamp object = new KeyAndEstCalendarTimestamp();
        object.setVal(Calendar.getInstance());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testEstDateTimestamp() {
        final KeyAndEstDateTimestamp object = new KeyAndEstDateTimestamp();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test
    public void testEstLongTimestamp() {
        final KeyAndEstLongTimestamp object = new KeyAndEstLongTimestamp();
        object.setVal(Calendar.getInstance().getTime().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStringNotTimestamp() {
        final KeyAndStringTimestamp object = new KeyAndStringTimestamp();
        object.setVal("NotTimestamp");
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test(expected = DynamoDBMappingException.class)
    public void testEmptyPattern() throws Exception {
        final KeyAndEmptyPattern object = new KeyAndEmptyPattern();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * Test timestamp formatting.
     */
    @Test(expected = DynamoDBMappingException.class)
    public void testInvalidPattern() throws Exception {
        final KeyAndInvalidPattern object = new KeyAndInvalidPattern();
        object.setVal(Calendar.getInstance().getTime());
        assertBeforeAndAfterChange(false, object);
    }

    /**
     * An object with {@code Calendar}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndCalendarTimestamp extends AutoKeyAndVal<Calendar> {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz")
        public Calendar getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Calendar val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndDateTimestamp extends AutoKeyAndVal<Date> {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Long}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndLongTimestamp extends AutoKeyAndVal<Long> {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz")
        public Long getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Long val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Calendar}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEstCalendarTimestamp extends AutoKeyAndVal<Calendar> {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public Calendar getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Calendar val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEstDateTimestamp extends AutoKeyAndVal<Date> {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Long}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEstLongTimestamp extends AutoKeyAndVal<Long> {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "America/New_York")
        public Long getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Long val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code String}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndStringTimestamp extends AutoKeyAndVal<String> {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz")
        public String getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final String val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEmptyPattern extends KeyAndDateTimestamp {
        @DynamoDBTypeConvertedTimestamp(pattern = "")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndEmptyTimeZone extends KeyAndDateTimestamp {
        @DynamoDBTypeConvertedTimestamp(pattern = "yyyyMMddHHmmssSSSz", timeZone = "")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

    /**
     * An object with {@code Date}.
     */
    @DynamoDBTable(tableName = "aws-java-sdk-util")
    public static class KeyAndInvalidPattern extends KeyAndDateTimestamp {
        @DynamoDBTypeConvertedTimestamp(pattern = "invalid")
        public Date getVal() {
            return super.getVal();
        }

        @Override
        public void setVal(final Date val) {
            super.setVal(val);
        }
    }

}
