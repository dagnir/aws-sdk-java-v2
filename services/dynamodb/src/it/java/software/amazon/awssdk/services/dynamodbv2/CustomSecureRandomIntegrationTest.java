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

package software.amazon.awssdk.services.dynamodbv2;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.security.SecureRandom;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class CustomSecureRandomIntegrationTest extends AWSIntegrationTestBase {

    @Test
    public void customSecureRandomConfigured_UsesCustomImplementation() {
        CustomSecureRandomImpl customSecureRandom = spy(new CustomSecureRandomImpl());
        AmazonDynamoDB ddb = new AmazonDynamoDBClient(getCredentials(),
                                                      new ClientConfiguration().withSecureRandom(customSecureRandom));
        ddb.listTables();
        verify(customSecureRandom, atLeastOnce()).nextBytes((byte[]) Mockito.any());
    }

    private static class CustomSecureRandomImpl extends SecureRandom {

        private static final long serialVersionUID = -4970552916858838974L;
    }

}
