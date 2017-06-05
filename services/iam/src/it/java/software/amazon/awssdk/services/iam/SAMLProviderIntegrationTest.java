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

package software.amazon.awssdk.services.iam;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.iam.model.CreateSAMLProviderRequest;
import software.amazon.awssdk.services.iam.model.DeleteSAMLProviderRequest;
import software.amazon.awssdk.services.iam.model.GetSAMLProviderRequest;
import software.amazon.awssdk.services.iam.model.ListSAMLProvidersRequest;
import software.amazon.awssdk.services.iam.model.ListSAMLProvidersResult;

/** Integration tests of the SAML provider APIs. */
public class SAMLProviderIntegrationTest extends IntegrationTestBase {

    private static final String fakeProviderARN = "arn:aws:sts::599169622985:saml-provider/ImaginaryProvider";
    private static String fakeIdPMetadataDocumentContent;

    static {
        int DOCUMENT_LENGTH = 1200; // The api requires a metadata document with
        // minimum length of 1000 characters.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DOCUMENT_LENGTH; i++) {
            sb.append("a");
        }
        fakeIdPMetadataDocumentContent = sb.toString();
    }

    @Test
    public void testOperations() {
        // Currently there is no easy way to test creating a new SAML
        // provider, as it requires we setup an IdP and generate a valid
        // metadata xml file which could be used by IAM. Here we just submit
        // an invalid metadata file, and expect the service to return 
        // InvalidInput error
        try {
            iam.createSAMLProvider(CreateSAMLProviderRequest.builder().name(
                    "fake-idp").samlMetadataDocument(fakeIdPMetadataDocumentContent).build());
            Assert.fail("Service exception is expected.");
        } catch (AmazonServiceException ase) {
            Assert.assertEquals("InvalidInput", ase.getErrorCode());
            Assert.assertTrue(ase.getMessage().contains("Could not parse metadata"));
        }

        // No SAML providers
        ListSAMLProvidersResult listSAMLProvidersResult = iam.listSAMLProviders(ListSAMLProvidersRequest.builder().build());
        Assert.assertEquals(0, listSAMLProvidersResult.samlProviderList().size());

        // Get/delete the imaginary SAML provider
        try {
            iam.getSAMLProvider(GetSAMLProviderRequest.builder().samlProviderArn(fakeProviderARN).build());
        } catch (AmazonServiceException ase) {
            Assert.assertEquals("NoSuchEntity", ase.getErrorCode());
        }

        try {
            iam.deleteSAMLProvider(DeleteSAMLProviderRequest.builder().samlProviderArn(fakeProviderARN).build());
        } catch (AmazonServiceException ase) {
            Assert.assertEquals("NoSuchEntity", ase.getErrorCode());
        }

    }
}
