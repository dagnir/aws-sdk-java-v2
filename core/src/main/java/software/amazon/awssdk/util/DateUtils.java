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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.ThreadSafe;

/**
 * Utilities for parsing and formatting dates. All {@code Instant}s are
 * formatted using the GMT timezone.
 * <p>
 *     The following format patterns are used:
 *     <ul>
 *         <li>RFC 822: {@code "EEE, dd MMM y HH:mm:ss 'GMT'"}</li>
 *         <li>ISO 8601: {@code "y-MM-dd'T'HH:mm:ss[.SSS]'Z'"}</li>
 *         <li>ISO 8601 Compressed: {@code "yMMdd'T'HHmmss'Z'"}</li>
 *     </ul>
 * </p>
 *
 * See {@link DateTimeFormatter} for more information on format patterns.
 * <p>
 *     Since all {@code Instant}s are formatted and parsed as datetimes, they
 *     must be less than or equal to {@link java.time.LocalDateTime#MAX}, which
 *     is 1 year less than {@link Instant#MAX}.
 * </p>
 */
@ThreadSafe
public final class DateUtils {
    private static final DateTimeFormatter RFC_822_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM y HH:mm:ss 'GMT'")
            .withLocale(Locale.US)
            .withZone(ZoneId.of("GMT"));

    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter
            .ofPattern("y-MM-dd'T'HH:mm:ss[.SSS]'Z'")
            .withZone(ZoneId.of("GMT"));

    private static final DateTimeFormatter ISO_8601_COMPRESSED_FORMATTER = DateTimeFormatter
            .ofPattern("yMMdd'T'HHmmss'Z'")
            .withZone(ZoneId.of("GMT"));

    private static final int AWS_DATE_MILLISECOND_PRECISION = 3;

    private DateUtils() {
    }

    /**
     * Parse the specified ISO 8601 formatted date string.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed date.
     */
    public static Instant parseIso8601Date(String dateString) {
        return Instant.from(ISO_8601_FORMATTER.parse(dateString));
    }

    /**
     * Format the specified {@code Instant} to an ISO 8601 date string in the
     * GMT timezone.
     *
     * @param instant
     *            The {@code Instant} to format.
     *
     * @return The formatted {@code Instant}.
     */
    public static String formatIso8601Date(Instant instant) {
        return ISO_8601_FORMATTER.format(instant);
    }

    /**
     * Parse the specified RFC 822 formatted date string.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed {@code Instant}.
     */
    public static Instant parseRfc822Date(String dateString) {
        if (dateString == null) {
            return null;
        }
        return Instant.from(RFC_822_FORMATTER.parse(dateString));
    }

    /**
     * Format the specified {@code Instant} as an RFC 822 date string in the
     * GMT timezone.
     *
     * @param instant
     *            The date to format.
     *
     * @return The formatted {@code Instant}.
     */
    public static String formatRfc822Date(Instant instant) {
        return RFC_822_FORMATTER.format(instant);
    }

    /**
     * Parse the specified compressed ISO 8601 date string.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed {@code Instant}.
     */
    public static Instant parseCompressedIso8601Date(String dateString) {
        return Instant.from(ISO_8601_COMPRESSED_FORMATTER.parse(dateString));
    }

    /**
     * Parses the given date string returned by the AWS service.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed {@code Instant}.
     */
    public static Instant parseServiceSpecificDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            BigDecimal dateValue = new BigDecimal(dateString);
            return Instant.ofEpochMilli(dateValue.scaleByPowerOfTen(
                    AWS_DATE_MILLISECOND_PRECISION).longValue());


        } catch (NumberFormatException nfe) {
            throw new SdkClientException("Unable to parse date : " + dateString, nfe);
        }
    }

    /**
     * Format the given {@code Instant} to an AWS service specific date string.
     *
     * @param instant
     *            The {@code Instant} to format.
     *
     * @return The formatted {@link Instant}.
     */
    public static String formatServiceSpecificDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        BigDecimal dateValue = BigDecimal.valueOf(instant.toEpochMilli());
        return dateValue.scaleByPowerOfTen(0 - AWS_DATE_MILLISECOND_PRECISION)
                        .toPlainString();
    }

    /**
     * Returns the number of days since epoch with respect to the given number
     * of milliseconds since epoch.
     */
    public static long numberOfDaysSinceEpoch(long milliSinceEpoch) {
        return ChronoUnit.DAYS.between(Instant.EPOCH, Instant.ofEpochMilli(milliSinceEpoch));
    }
}
