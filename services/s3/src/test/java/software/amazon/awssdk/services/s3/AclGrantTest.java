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
        Assert.assertEquals(1,acl.getGrants().size());
        Assert.assertEquals(1,acl.getGrantsAsList().size());

        acl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);
        acl.grantPermission(new EmailAddressGrantee("aws-dr-eclipse@amazon.com"), Permission.Read);

        Assert.assertEquals(3,acl.getGrantsAsList().size());

    }
}
