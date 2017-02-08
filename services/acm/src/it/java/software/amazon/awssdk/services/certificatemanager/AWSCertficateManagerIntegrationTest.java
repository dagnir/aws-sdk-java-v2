/*
* Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights
* Reserved.
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

package software.amazon.awssdk.services.certificatemanager;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.certificatemanager.model.GetCertificateRequest;
import software.amazon.awssdk.services.certificatemanager.model.ListCertificatesRequest;
import software.amazon.awssdk.services.certificatemanager.model.ListCertificatesResult;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class AWSCertficateManagerIntegrationTest extends
        AWSIntegrationTestBase {

    private static AWSCertificateManager client;

    @BeforeClass
    public static void setUp() {
        client = new AWSCertificateManagerClient(getCredentials());
    }

    @Test
    public void list_certificates() {
        ListCertificatesResult result = client.listCertificates(new
                ListCertificatesRequest());
        Assert.assertTrue(result.getCertificateSummaryList().size() >= 0);
    }

    /**
     * Ideally the service must be throwing a Invalid Arn exception
     * instead of AmazonServiceException. Have reported this to service to
     * fix it.
     *  TODO Change the expected when service fix this.
     */
    @Test(expected = AmazonServiceException.class)
    public void get_certificate_fake_arn_throws_exception() {
        client.getCertificate(new GetCertificateRequest().withCertificateArn
                ("arn:aws:acm:us-east-1:123456789:fakecert"));
    }


}
