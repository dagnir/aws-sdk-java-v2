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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.List;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.junit.Test;

public class MimetypesTest {

    private static MBeanServer getMBeanServer() {
        List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        MBeanServer server = servers.size() > 0
                             ? servers.get(0)
                             : ManagementFactory.getPlatformMBeanServer();
        return server;
    }

    /** Tests that we aren't leaking file descriptors. */
    @Test
    public void testFileDescriptorLeaks() throws Exception {
        long fd = getOpenFileDecriptorCount();
        Mimetypes mt = Mimetypes.getInstance();
        long fd2 = getOpenFileDecriptorCount();
        // If fd2 is greater than fd, then we've detected a leak
        assertFalse(fd2 > fd);

        InputStream is = Mimetypes.class.getResourceAsStream("/mime.types");
        long fd3 = getOpenFileDecriptorCount();
        // An additional fd still opened
        assertTrue(fd2 < fd3);
        mt.loadAndReplaceMimetypes(is);
        long fd4 = getOpenFileDecriptorCount();
        assertTrue("fd3=" + fd3 + ", fd4=" + fd4, fd3 >= fd4);
        is.close();
        // All additional fd closed
        long fd5 = getOpenFileDecriptorCount();
        assertFalse("fd5=" + fd5 + ", fd=" + fd, fd5 > fd);
    }

    /** Tests that file names with mixed case are detected correctly. */
    @Test
    public void testExtensionsWithCaps() throws Exception {
        Mimetypes mt = Mimetypes.getInstance();
        assertEquals("image/jpeg", mt.getMimetype("image.JPeG"));
    }

    private long getOpenFileDecriptorCount() throws Exception {
        MBeanServer mbsc = getMBeanServer();
        AttributeList attributes;
        attributes = mbsc.getAttributes(
                new ObjectName("java.lang:type=OperatingSystem"),
                new String[] {"OpenFileDescriptorCount"});
        List<Attribute> attrList = attributes.asList();
        return (Long) attrList.get(0).getValue();
    }
}
