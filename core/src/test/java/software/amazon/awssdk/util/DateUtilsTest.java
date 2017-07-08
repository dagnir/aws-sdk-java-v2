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

package software.amazon.awssdk.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonFactory;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import software.amazon.awssdk.protocol.json.SdkJsonGenerator;
import software.amazon.awssdk.protocol.json.StructuredJsonGenerator;

public class DateUtilsTest {
    private static final Instant REFERENCE_INSTANT = Instant.ofEpochMilli(123456789L);
    private static final String FORMATTED_ISO8601 = "1970-01-02T10:17:36.789Z";
    private static final String FORMATTED_ISO8601_COMPRESSED = "19700102T101736Z";
    private static final String FORMATTED_ISO8601_ALTERNATIVE = "1970-01-02T10:17:36Z";
    private static final String FORMATTED_RFC822 = "Fri, 02 Jan 1970 10:17:36 GMT";

    @Test
    public void formatIso8601Date() throws ParseException {
        assertThat(DateUtils.formatIso8601Date(REFERENCE_INSTANT)).isEqualTo(FORMATTED_ISO8601);
    }

    @Test
    public void formatRfc822Date() throws ParseException {
        assertThat(DateUtils.formatRfc822Date(REFERENCE_INSTANT)).isEqualTo(FORMATTED_RFC822);
    }

    @Test
    public void parseCompressedIso8601Date() throws ParseException {
        assertThat(DateUtils.parseCompressedIso8601Date(FORMATTED_ISO8601_COMPRESSED))
                .isEqualTo(REFERENCE_INSTANT.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    public void parseRfc822Date() throws ParseException {
        assertThat(DateUtils.parseRfc822Date(FORMATTED_RFC822)).isEqualTo(REFERENCE_INSTANT.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    public void parseIso8601Date() throws ParseException {
        assertThat(DateUtils.parseIso8601Date(FORMATTED_ISO8601)).isEqualTo(REFERENCE_INSTANT);
    }

    @Test
    public void parseIso8601Date_usingAlternativeFormat() throws ParseException {
        assertThat(DateUtils.parseIso8601Date(FORMATTED_ISO8601_ALTERNATIVE))
                .isEqualTo(REFERENCE_INSTANT.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    public void canRoundTripMaxDateTime() {
        Instant max = LocalDateTime.MAX.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        assertThat(DateUtils.parseIso8601Date(DateUtils.formatIso8601Date(max))).isEqualTo(max);
    }

    @Test(expected = DateTimeParseException.class)
    public void invalidDate() throws ParseException {
        final String input = "2014-03-06T14:28:58.000Z.000Z";
        DateUtils.parseIso8601Date(input);
    }

    /**
     * Tests the Date marshalling and unmarshalling. Asserts that the value is
     * same before and after marshalling/unmarshalling
     */
    @Test
    public void testAwsFormatDateUtils() throws Exception {
        testDate(System.currentTimeMillis());
        testDate(1L);
        testDate(0L);
    }

    private void testDate(long dateInMilliSeconds) {
        String serverSpecificDateFormat = DateUtils
                .formatServiceSpecificDate(Instant.ofEpochMilli(dateInMilliSeconds));

        Instant parsedDate = DateUtils.parseServiceSpecificDate(String
                                                                     .valueOf(serverSpecificDateFormat));

        assertThat(Long.valueOf(parsedDate.toEpochMilli())).isEqualTo(Long.valueOf(dateInMilliSeconds));
    }

    // See https://forums.aws.amazon.com/thread.jspa?threadID=158756
    @Test
    public void testNumericNoQuote() {
        StructuredJsonGenerator jw = new SdkJsonGenerator(new JsonFactory(), null);
        jw.writeStartObject();
        jw.writeFieldName("foo").writeValue(Instant.now());
        jw.writeEndObject();
        String s = new String(jw.getBytes(), Charset.forName("UTF-8"));
        // Something like: {"foo":1408378076.135}.
        // Note prior to the changes, it was {"foo":1408414571}
        // (with no decimal point nor places.);
        final String prefix = "{\"foo\":";
        assertThat(s.startsWith(prefix)).withFailMessage(s).isTrue();
        final int startPos = prefix.length();
        // verify no starting quote for the value
        assertThat(s.startsWith("{\"foo\":\"")).withFailMessage(s).isFalse();
        assertThat(s.endsWith("}")).withFailMessage(s).isTrue();
        // Not: {"foo":"1408378076.135"}.
        // verify no ending quote for the value
        assertThat(s.endsWith("\"}")).withFailMessage(s).isFalse();
        final int endPos = s.indexOf("}");
        final int dotPos = s.length() - 5;
        assertThat(s.charAt(dotPos) == '.').withFailMessage(s).isTrue();
        // verify all numeric before '.'
        char[] a = s.toCharArray();
        for (int i = startPos; i < dotPos; i++) {
            assertThat(a[i] <= '9' && a[i] >= '0').isTrue();
        }
        int j = 0;
        // verify all numeric after '.'
        for (int i = dotPos + 1; i < endPos; i++) {
            assertThat(a[i] <= '9' && a[i] >= '0').isTrue();
            j++;
        }
        // verify decimal precision of exactly 3
        assertThat(j).isEqualTo(3);
    }

    @Test
    public void numberOfDaysSinceEpoch() {
        final long now = System.currentTimeMillis();
        final long days = DateUtils.numberOfDaysSinceEpoch(now);
        final long oneDayMilli = Duration.ofDays(1).toMillis();

        assertThat(now > days * oneDayMilli).isTrue();
        assertThat((now - days * oneDayMilli) <= oneDayMilli).isTrue();
    }
}
