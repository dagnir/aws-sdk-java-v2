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

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.EmailAddressGrantee;
import software.amazon.awssdk.services.s3.model.Permission;

/**
 * Unit test for ACLGrants. Grants were stored as Set in the SDK while Amazon S3
 * allows duplicate grants. A backward compatible change was made to allow
 * duplicate grants to be sent and received from Amazon A3.
 */
public class AclGrantTest {

    @Test
    public void testGrants() {
        AccessControlList acl = new AccessControlList();
        acl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);
        acl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);
        acl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);

        Assert.assertEquals(3, acl.getGrantsAsList().size());
        Assert.assertEquals(1, acl.getGrants().size());
        Assert.assertEquals(1, acl.getGrantsAsList().size());

        acl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);
        acl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);

        Assert.assertEquals(3, acl.getGrantsAsList().size());

    }
}
