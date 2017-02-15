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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.s3.model.Tag;

/**
 * Contains utility methods for object tagging tests.
 */
public class ObjectTaggingTestUtil {

    public static Map<String, List<String>> convertTagSetToMap(List<Tag> tagSet) {
        Map<String, List<String>> tagMap = new HashMap<String, List<String>>();

        for (Tag t : tagSet) {
            List<String> values = tagMap.get(t.getKey());
            if (values == null) {
                values = new ArrayList<String>();
                tagMap.put(t.getKey(), values);
            }
            values.add(t.getValue());
        }

        for (List<String> tagValues : tagMap.values()) {
            Collections.sort(tagValues, String.CASE_INSENSITIVE_ORDER);
        }

        return tagMap;
    }

    public static void assertTagSetsAreEquals(List<Tag> expected, List<Tag> actual) {
        assertEquals(convertTagSetToMap(expected), convertTagSetToMap(actual));
    }
}
