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

package software.amazon.awssdk.services.s3.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SpecialObjectKeyNameGenerator {

    /** Each special character will be used to construct a special key name. **/
    private static final String specialCharacters = "~!@#$%^&*()_={}[]::'\\|`?<>,.";

    /** Each special key name pattern will be tested. **/
    private static final String[] specialKeyPatterns = new String[] {
        /* Leading slash */
        "/foo",
        "//foo",
        "///foo",
        "////foo",
        "%2Ffoo",
        "/%2F/foo",
        /* Slash in the middle */
        "foo/bar",
        "foo//bar",
        "foo///bar",
        "foo////bar",
        "foo/bar//charlie",
        "/foo/bar",
        "//foo/bar",
        "//foo//bar",
        };

    public static List<String> initAllSpecialKeyNames() {
        List<String> allSpecialKeyNames = new LinkedList<String>();
        allSpecialKeyNames.addAll(Arrays.asList(specialKeyPatterns));

        String baseKeyName = "foo";
        for (int i = 0; i < specialCharacters.length(); i++) {
            allSpecialKeyNames.add(specialCharacters.charAt(i) + baseKeyName);
        }

        return allSpecialKeyNames;
    }
}
