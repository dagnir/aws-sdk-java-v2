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

package software.amazon.awssdk.services.s3.internal;

import org.junit.Assert;
import org.junit.Test;

public class BucketNameUtilsTest {

    @Test
    public void bucketnameLessThan3Chars_NotDNSCompatible() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("ab"));
    }

    @Test
    public void bucketnameGreaterThan63Chars_NotDNSCompatible() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName
                ("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrst"));
    }

    @Test
    public void bucketNameWithMultipleLabelsSeparatedBySingleDot_validV2BucketName() {
        Assert.assertTrue(BucketNameUtils.isValidV2BucketName
                ("abc.def.ghi"));
    }

    @Test
    public void bucketNameWithMultipleLabelsSeparatedByMoreThanOneDot_notValidV2BucketName() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName
                ("abc..def.ghi"));
    }

    @Test
    public void bucketNameWithMultipleLabelsSeparatedBySingleDot_labelsStartWithUppercase_notValidV2BucketName() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName
                ("abc.Def.ghi"));
    }

    @Test
    public void bucketNameWithMultipleLabelsSeparatedBySingleDot_labelsEndWithUppercase_notValidV2BucketName() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName
                ("abc.deF.ghi"));
    }

    @Test
    public void bucketNameWithMultipleLabelsSeparatedBySingleDot_labelsWithNumbers_ValidV2BucketName() {
        Assert.assertTrue(BucketNameUtils.isValidV2BucketName
                ("abc.de1fg.ghi"));
        Assert.assertTrue(BucketNameUtils.isValidV2BucketName
                ("abc.1def.ghi"));
        Assert.assertTrue(BucketNameUtils.isValidV2BucketName
                ("abc.def12.ghi"));
    }

    @Test
    public void bucketnameStartWithUppercaseLetters_NotDNSCompatible() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("MyAwsbucket"));
    }

    @Test
    public void bucketNameStartsWithDot_notValidV2bucket() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName(".myawsbucket"));
    }

    @Test
    public void bucketNameEndsWithDot_notValidV2bucket() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("myawsbucket."));
    }

    @Test
    public void bucketNameWithMultipleDots_notValidV2bucket() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("my..examplebucket"));
    }

    @Test
    public void bucketNameContainsHyphen_notValidV2Bucket() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("-foo"));
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("foo.-bar"));
    }

    @Test
    public void bucketNameContainsSpecialCharacter_notValidV2Bucket() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("&foo"));
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("foo&bar"));
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("foo!bar"));
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("foobar~"));
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("foo.bar&"));
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("!foo.bar"));
    }

    @Test
    public void bucketNameContainsIPAddress_notValidV2Bucket() {
        Assert.assertFalse(BucketNameUtils.isValidV2BucketName("192.168.1.1"));
    }
}
