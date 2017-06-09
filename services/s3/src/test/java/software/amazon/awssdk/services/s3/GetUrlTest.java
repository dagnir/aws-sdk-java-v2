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

package software.amazon.awssdk.services.s3;


import static org.junit.Assert.assertEquals;

import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class GetUrlTest {

    private AmazonS3 s3;

    @Before
    public void setup() {
        s3 = AmazonS3ClientBuilder.standard()
                                  .withCredentials(new StaticCredentialsProvider(new AwsCredentials("akid", "skid")))
                                  .withRegion(Region.US_WEST_2)
                                  .build();
    }

    @Test
    public void keyWithSpaces_CorrectlyUrlEncoded() {
        final URL resourceUrl = s3.getUrl("foo-bucket", "key with spaces");
        assertEquals("https://foo-bucket.s3-us-west-2.amazonaws.com/key%20with%20spaces", resourceUrl.toExternalForm());
    }

    @Test
    public void normalKeyName_NotEncoded() {
        final URL resourceUrl = s3.getUrl("foo-bucket", "key-without-spaces");
        assertEquals("https://foo-bucket.s3-us-west-2.amazonaws.com/key-without-spaces", resourceUrl.toExternalForm());
    }

}
