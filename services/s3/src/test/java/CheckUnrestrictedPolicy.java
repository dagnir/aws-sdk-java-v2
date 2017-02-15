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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.crypto.KeyGenerator;
import org.junit.Test;

public class CheckUnrestrictedPolicy {

    @Test
    public void test() {
        boolean isUnlimitedSupported = false;
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES", "SunJCE");
            kgen.init(256);
            isUnlimitedSupported = true;
        } catch (NoSuchAlgorithmException e) {
            isUnlimitedSupported = false;
        } catch (NoSuchProviderException e) {
            isUnlimitedSupported = false;
        }
        System.out.println("isUnlimitedSupported=" + isUnlimitedSupported);
    }

}
