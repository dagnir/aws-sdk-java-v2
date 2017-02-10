/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.s3.model.transform;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.s3.ObjectTaggingTestUtil;
import software.amazon.awssdk.services.s3.model.Tag;

public class GetObjectTaggingResponseUnmarshallerTest {
    private static final String TEST_RESULT_FILE = "GetObjectTagsResponse.xml";
    @Test
    public void testUnmarshallResult() throws IOException {
        List<Tag> expectedTagSet = Arrays.asList(
                new Tag("Foo", "1"),
                new Tag("Bar", "2"),
                new Tag("Baz", "3")
        );
        List<Tag> receivedTagSet = new XmlResponsesSaxParser()
                .parseObjectTaggingResponse(getClass().getResourceAsStream(TEST_RESULT_FILE))
                .getResult().getTagSet();

        assertEquals(ObjectTaggingTestUtil.convertTagSetToMap(expectedTagSet),
                ObjectTaggingTestUtil.convertTagSetToMap(receivedTagSet));
    }
}
