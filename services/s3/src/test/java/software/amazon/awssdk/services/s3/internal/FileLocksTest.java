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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.UUID;
import org.junit.Test;

public class FileLocksTest {

    @Test
    public void test() {
        File file = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        assertFalse(file.exists());
        file.deleteOnExit();

        FileLocks.lock(file);
        assertTrue(file.exists());

        assertTrue(FileLocks.isFileLocked(file));

        FileLocks.unlock(file);
        assertFalse(FileLocks.isFileLocked(file));

        FileLocks.lock(file);
        assertTrue(FileLocks.isFileLocked(file));
        FileLocks.unlock(file);
    }
}
