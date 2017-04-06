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

package software.amazon.awssdk.services.lambda.invoke;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.lambda.model.InvokeAsyncRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.util.IoUtils;
import software.amazon.awssdk.util.StringUtils;

public class InvokeArgsAsStringTest {

    private static final String ARGS = "{ a : 'a', b : 'b' }";

    @Test
    public void testInvokeAsyncArgsAsString() throws IOException {
        InvokeAsyncRequest request = new InvokeAsyncRequest();
        request.setInvokeArgs(ARGS);

        InputStream stream = request.getInvokeArgs();

        String decoded = new String(IoUtils.toByteArray(stream), StringUtils.UTF8);

        Assert.assertEquals(ARGS, decoded);
    }

    @Test
    public void testInvokeArgsAsString() {
        InvokeRequest request = new InvokeRequest();
        request.setPayload(ARGS);

        ByteBuffer bb = request.getPayload();
        String decoded = StringUtils.UTF8.decode(bb).toString();

        Assert.assertEquals(ARGS, decoded);
    }
}
