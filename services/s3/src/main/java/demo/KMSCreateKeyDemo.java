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

package demo;

import java.io.IOException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.kms.model.CreateKeyResult;

public class KMSCreateKeyDemo {

    public static void main(String[] args) throws IOException {
        AWSKMSClient kms = new AWSKMSClient()
             .withRegion(Region.getRegion(Regions.US_WEST_2));
        CreateKeyResult result = kms.createKey();
        System.err.println(result);
        kms.shutdown();
    }
    // {KeyMetadata: {AWSAccountId: 814230673633,
    // KeyId: fc763590-3758-4e2f-9da6-303ecbfe37eb,
    // Arn: arn:aws:kms:us-west-2:814230673633:key/fc763590-3758-4e2f-9da6-303ecbfe37eb,
    // CreationDate: Tue Nov 18 09:38:36 PST 2014,Enabled: true,Description: ,KeyUsage: ENCRYPT_DECRYPT}}
}
