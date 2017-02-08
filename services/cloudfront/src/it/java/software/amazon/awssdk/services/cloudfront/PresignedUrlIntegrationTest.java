package software.amazon.awssdk.services.cloudfront;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.test.util.DateUtils.yyMMdd_hhmmss;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.cloudfront.CloudFrontCookieSigner.CookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.model.Aliases;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CacheBehaviors;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontOriginAccessIdentityConfig;
import software.amazon.awssdk.services.cloudfront.model.CookiePreference;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityResult;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResult;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DeleteDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.ForwardedValues;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigResult;
import software.amazon.awssdk.services.cloudfront.model.ItemSelection;
import software.amazon.awssdk.services.cloudfront.model.LoggingConfig;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.PriceClass;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;
import software.amazon.awssdk.services.cloudfront.model.TrustedSigners;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionResult;
import software.amazon.awssdk.services.cloudfront.util.SignerUtils;
import software.amazon.awssdk.services.cloudfront.util.SignerUtils.Protocol;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.CanonicalGrantee;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.StringUtils;

/**
 * Tests pre-signed URLs
 */
public class PresignedUrlIntegrationTest extends IntegrationTestBase {

    private static final String PRIVATE_KEY_FILE = "pk-APKAJM22QV32R3I2XVIQ.pem";
//    private static final String PRIVATE_KEY_FILE_DER = "pk-APKAJM22QV32R3I2XVIQ.der";
    private static final String PRIVATE_KEY_ID = "APKAJM22QV32R3I2XVIQ";
    private static String callerReference = yyMMdd_hhmmss();
    private static final String bucketName = StringUtils.lowerCase(CloudFrontIntegrationTest.class.getSimpleName())
            + "." + callerReference;
    private static final String DEFAULT_ROOT_OBJECT = "key.html";

    private static String dnsName;
    private static final String DISTRIBUTION_COMMENT = "comment";
    private static String domainName;

    private static String distributionETag;
    private static String distributionId;

    @BeforeClass
    public static void initial() throws Exception {
        IntegrationTestBase.setUp();

        CreateCloudFrontOriginAccessIdentityResult result =
            cloudfront.createCloudFrontOriginAccessIdentity(
                new CreateCloudFrontOriginAccessIdentityRequest(
                    new CloudFrontOriginAccessIdentityConfig()
                        .withComment("new access identity")
                        .withCallerReference(callerReference)));

        String s3CanonicalUserId = result.getCloudFrontOriginAccessIdentity()
                                         .getS3CanonicalUserId();
        System.out.println("s3CanonicalUserId=" + s3CanonicalUserId);
        String accessId = result.getCloudFrontOriginAccessIdentity().getId();
        System.out.println("accessId=" + accessId);

        s3.createBucket(bucketName);

        dnsName = bucketName + ".s3.amazonaws.com";
        File content = new RandomTempFile("" + System.currentTimeMillis(),
                1000L);

        s3.putObject(bucketName, "key", content);
        AccessControlList acl = s3.getObjectAcl(bucketName, "key");
        acl.grantPermission(new CanonicalGrantee(s3CanonicalUserId), Permission.Read);
        s3.setObjectAcl(bucketName, "key", acl);

        // create a private distribution
        CreateDistributionResult createDistributionResult = cloudfront
            .createDistribution(
                new CreateDistributionRequest()
                .withDistributionConfig(
                    new DistributionConfig()
                    .withPriceClass(PriceClass.PriceClass_100)
                    .withDefaultCacheBehavior(
                        new DefaultCacheBehavior()
                        .withMinTTL(100L)
                        .withTargetOriginId("1")
                        .withViewerProtocolPolicy("allow-all")
                        .withTrustedSigners(new TrustedSigners().withEnabled(true).withQuantity(1).withItems("self"))
                        .withForwardedValues(
                            new ForwardedValues()
                            .withCookies(
                                new CookiePreference()
                                .withForward(ItemSelection.None))
                            .withQueryString(true)))
                        .withAliases(new Aliases().withQuantity(0))
                    .withLogging(
                        new LoggingConfig()
                        .withIncludeCookies(Boolean.FALSE)
                        .withEnabled(false)
                        .withBucket(bucketName)
                        .withPrefix(""))
                    .withCallerReference(callerReference)
                    .withCacheBehaviors(
                         new CacheBehaviors()
                         .withQuantity(1)
                         .withItems(
                             new CacheBehavior()
                             .withMinTTL(100L)
                             .withTargetOriginId("1")
                             .withViewerProtocolPolicy("allow-all")
                             .withTrustedSigners(new TrustedSigners().withEnabled(true).withQuantity(1).withItems("self"))
                             .withForwardedValues(
                                 new ForwardedValues()
                                 .withCookies(
                                     new CookiePreference()
                                     .withForward(ItemSelection.None))
                                 .withQueryString(true))
                             .withPathPattern("*")))
                    .withComment(DISTRIBUTION_COMMENT)
                    .withDefaultRootObject(DEFAULT_ROOT_OBJECT)
                    .withEnabled(true)
                    .withOrigins(
                        new Origins()
                        .withItems(
                            new Origin()
                            .withDomainName(dnsName)
                            .withId("1")
                            .withS3OriginConfig(new S3OriginConfig().withOriginAccessIdentity("origin-access-identity/cloudfront/" + accessId)))
                    .withQuantity(1))));

        domainName = createDistributionResult.getDistribution().getDomainName();
        distributionId = createDistributionResult.getDistribution().getId();
        distributionETag = createDistributionResult.getETag();

        waitForDistributionToDeploy(distributionId);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // Disable the distribution
        GetDistributionConfigResult distributionConfigResults = cloudfront.getDistributionConfig(new GetDistributionConfigRequest().withId(distributionId));
        DistributionConfig distributionConfig = distributionConfigResults.getDistributionConfig();
        distributionConfig.setEnabled(false);
        UpdateDistributionResult updateDistributionResult = cloudfront
                .updateDistribution(new UpdateDistributionRequest().withId(distributionId)
                        .withIfMatch(distributionETag)
                        .withDistributionConfig(distributionConfig));
     distributionETag = updateDistributionResult.getETag();

     waitForDistributionToDeploy(distributionId);

        if (distributionId != null) {
            try {

                 cloudfront.deleteDistribution(new DeleteDistributionRequest().withId(distributionId).withIfMatch(
                         distributionETag));
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        deleteBucketAndAllContents(bucketName);
    }

    /**
     * Tests creating a pre-signed URL to access cloudfront content.
     */
    @Test
    public void testPreSignedUrl() throws Exception {

        File s3Object = File.createTempFile(this.getClass().getName(), "");
        s3.getObject(new GetObjectRequest(bucketName, "key"), s3Object);

        Date dateLessThan = new Date(System.currentTimeMillis() + 10 * 1000);
        String cannedSignedURL = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(Protocol.https, domainName,
                new File("tst/" + PRIVATE_KEY_FILE), "key", PRIVATE_KEY_ID, dateLessThan);
        System.err.println("cannedSignedURL_der: " + cannedSignedURL);

//        String cannedSignedURL_der = CloudFrontUrlSigner.getCannedSignedURL(Protocol.https, domainName,
//                new File("tst/" + PRIVATE_KEY_FILE_DER), "key", PRIVATE_KEY_ID, dateLessThan);
//        assertEquals(cannedSignedURL_der, cannedSignedURL);
//        System.err.println("cannedSignedURL_der: " + cannedSignedURL_der);

        HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(new HttpGet(cannedSignedURL));
        assertEquals(200, response.getStatusLine().getStatusCode());

        InputStream inputStream = response.getEntity().getContent();

        assertFileEqualsStream(s3Object, inputStream);
    }

    @Test
    public void testUnsignedUri() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(SignerUtils.generateResourcePath(Protocol.https, domainName, "key"));
        HttpResponse response = client.execute(httpGet);

        assertEquals(403, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testSignedCookie() throws Exception {
        File s3Object = File.createTempFile(this.getClass().getName(), "");
        s3.getObject(new GetObjectRequest(bucketName, "key"), s3Object);

        Date dateLessThan = new Date(System.currentTimeMillis() + 10 * 1000);
        CookiesForCannedPolicy cookies = CloudFrontCookieSigner.getCookiesForCannedPolicy(Protocol.https, domainName,
                new File("tst/" + PRIVATE_KEY_FILE), "key", PRIVATE_KEY_ID, dateLessThan);

        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(SignerUtils.generateResourcePath(Protocol.https, domainName, "key"));

        httpGet.addHeader("Cookie", cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue());
        httpGet.addHeader("Cookie", cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue());
        httpGet.addHeader("Cookie", cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue());
        HttpResponse response = client.execute(httpGet);

        assertEquals(200, response.getStatusLine().getStatusCode());

        InputStream inputStream = response.getEntity().getContent();
        assertFileEqualsStream(s3Object, inputStream);
    }

}
