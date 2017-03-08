package software.amazon.awssdk.services.securitytoken.auth;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.services.securitytoken.model.AssumeRoleRequest;
import software.amazon.awssdk.services.securitytoken.model.transform.AssumeRoleRequestMarshaller;

public class AssumeRoleWithMfaTest {
    @Test
    public void testMarshall() {
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
            .withRoleArn("arn:aws:iam::123456789012:role/test")
            .withExternalId("oogeyboogey")
            .withSerialNumber("arn:aws:iam::123456789012:mfa/test")
            .withTokenCode("000000");

        AssumeRoleRequestMarshaller marshaller =
            new AssumeRoleRequestMarshaller();

        Request<AssumeRoleRequest> request = marshaller.marshall(assumeRoleRequest);

        Map<String, List<String>> requestParams = request.getParameters();
        Assert.assertNotNull(requestParams.get("RoleArn"));
        Assert.assertTrue(1 == requestParams.get("RoleArn").size());

        Assert.assertEquals(
            "arn:aws:iam::123456789012:role/test",
            requestParams.get("RoleArn").iterator().next());

        Assert.assertNotNull(requestParams.get("ExternalId"));
        Assert.assertTrue(1 == requestParams.get("ExternalId").size());

        Assert.assertEquals(
            "oogeyboogey",
            requestParams.get("ExternalId").iterator().next());

        Assert.assertNotNull(requestParams.get("SerialNumber"));
        Assert.assertTrue(1 == requestParams.get("SerialNumber").size());
        Assert.assertEquals(
            "arn:aws:iam::123456789012:mfa/test",
            requestParams.get("SerialNumber").iterator().next());

        Assert.assertNotNull(requestParams.get("TokenCode"));
        Assert.assertTrue(1 == requestParams.get("TokenCode").size());
        Assert.assertEquals(
            "000000",
            requestParams.get("TokenCode").iterator().next());
    }
}
