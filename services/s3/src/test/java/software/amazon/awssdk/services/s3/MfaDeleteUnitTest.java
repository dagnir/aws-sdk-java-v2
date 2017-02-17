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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import javax.xml.xpath.XPath;
import org.junit.Test;
import org.w3c.dom.Document;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.transform.BucketConfigurationXmlFactory;
import software.amazon.awssdk.services.s3.model.transform.Unmarshallers.BucketVersioningConfigurationUnmarshaller;
import software.amazon.awssdk.util.XpathUtils;

/**
 * Unit tests for MFA Delete marshalling/unmarshalling.
 * <p>
 * MFA Delete is difficult to test since it requires a physical token generator,
 * so in addition to the manual acceptance tests that require multiple MFA
 * tokens to be entered while the test runs, these tests provide automated
 * testing for pieces of MFA Delete, without requiring a physical token
 * generator.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class MfaDeleteUnitTest {

    private static final String MFA_UNSPECIFIED_XML =
            "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
            "<Status>Enabled</Status>" +
            "</VersioningConfiguration>";

    private static final String MFA_ENABLED_XML =
            "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
            "<Status>Enabled</Status>" +
            "<MfaDelete>Enabled</MfaDelete>" +
            "</VersioningConfiguration>";

    private static final String MFA_DISABLED_XML =
            "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
            "<Status>Enabled</Status>" +
            "<MfaDelete>Disabled</MfaDelete>" +
            "</VersioningConfiguration>";

    /**
     * Tests that we can correctly marshall a bucket versioning configuration
     * object to XML.
     */
    @Test
    public void testBucketVersioningConfigurationMarshalling() throws Exception {
        BucketConfigurationXmlFactory xmlFactory = new BucketConfigurationXmlFactory();

        // Test the XML created when MFA Delete is unspecified
        BucketVersioningConfiguration versioningConfiguration =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        String xml = new String(xmlFactory.convertToXmlByteArray(versioningConfiguration));
        Document document = XpathUtils.documentFrom(xml);
        XPath xpath = XpathUtils.xpath();
        assertEquals("Enabled", XpathUtils.asString("/VersioningConfiguration/Status", document, xpath));
        assertEquals(null, XpathUtils.asString("/VersioningConfiguration/MfaDelete", document, xpath));

        // Test the XML created when MFA Delete is enabled
        versioningConfiguration.setMfaDeleteEnabled(Boolean.TRUE);
        xml = new String(xmlFactory.convertToXmlByteArray(versioningConfiguration));
        document = XpathUtils.documentFrom(xml);
        assertEquals("Enabled", XpathUtils.asString("/VersioningConfiguration/Status", document, xpath));
        assertEquals("Enabled", XpathUtils.asString("/VersioningConfiguration/MfaDelete", document, xpath));

        // Test the XML created when MFA Delete is disabled
        versioningConfiguration.setMfaDeleteEnabled(Boolean.FALSE);
        xml = new String(xmlFactory.convertToXmlByteArray(versioningConfiguration));
        document = XpathUtils.documentFrom(xml);
        assertEquals("Enabled", XpathUtils.asString("/VersioningConfiguration/Status", document, xpath));
        assertEquals("Disabled", XpathUtils.asString("/VersioningConfiguration/MfaDelete", document, xpath));
    }

    /**
     * Tests that we can correctly unmarshall a bucket versioning configuration
     * object from XML.
     */
    @Test
    public void testBucketVersioningConfigurationUnmarshalling() throws Exception {
        BucketVersioningConfigurationUnmarshaller unmarshaller = new BucketVersioningConfigurationUnmarshaller();
        BucketVersioningConfiguration bucketVersioningConfiguration;

        // Test an XML response when MFA Delete is unspecified
        bucketVersioningConfiguration = (BucketVersioningConfiguration) unmarshaller.unmarshall(new ByteArrayInputStream(MFA_UNSPECIFIED_XML.getBytes()));
        assertEquals(BucketVersioningConfiguration.ENABLED, bucketVersioningConfiguration.getStatus());
        assertNull(bucketVersioningConfiguration.isMfaDeleteEnabled());

        // Test the XML created when MFA Delete is enabled
        bucketVersioningConfiguration = (BucketVersioningConfiguration) unmarshaller.unmarshall(new ByteArrayInputStream(MFA_ENABLED_XML.getBytes()));
        assertEquals(BucketVersioningConfiguration.ENABLED, bucketVersioningConfiguration.getStatus());
        assertTrue(bucketVersioningConfiguration.isMfaDeleteEnabled().booleanValue());

        // Test the XML created when MFA Delete is disabled
        bucketVersioningConfiguration = (BucketVersioningConfiguration) unmarshaller.unmarshall(new ByteArrayInputStream(MFA_DISABLED_XML.getBytes()));
        assertEquals(BucketVersioningConfiguration.ENABLED, bucketVersioningConfiguration.getStatus());
        assertFalse(bucketVersioningConfiguration.isMfaDeleteEnabled().booleanValue());
    }

}
