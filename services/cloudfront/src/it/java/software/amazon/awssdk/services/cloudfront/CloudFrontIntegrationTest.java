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

package software.amazon.awssdk.services.cloudfront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.cloudfront.model.Aliases;
import software.amazon.awssdk.services.cloudfront.model.AllowedMethods;
import software.amazon.awssdk.services.cloudfront.model.CacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.CacheBehaviors;
import software.amazon.awssdk.services.cloudfront.model.CookiePreference;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateDistributionResult;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResult;
import software.amazon.awssdk.services.cloudfront.model.CustomErrorResponse;
import software.amazon.awssdk.services.cloudfront.model.CustomErrorResponses;
import software.amazon.awssdk.services.cloudfront.model.DefaultCacheBehavior;
import software.amazon.awssdk.services.cloudfront.model.DeleteDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.Distribution;
import software.amazon.awssdk.services.cloudfront.model.DistributionConfig;
import software.amazon.awssdk.services.cloudfront.model.ForwardedValues;
import software.amazon.awssdk.services.cloudfront.model.GeoRestriction;
import software.amazon.awssdk.services.cloudfront.model.GeoRestrictionType;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionConfigResult;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionResult;
import software.amazon.awssdk.services.cloudfront.model.GetInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.GetInvalidationResult;
import software.amazon.awssdk.services.cloudfront.model.Invalidation;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.ItemSelection;
import software.amazon.awssdk.services.cloudfront.model.ListDistributionsRequest;
import software.amazon.awssdk.services.cloudfront.model.ListDistributionsResult;
import software.amazon.awssdk.services.cloudfront.model.ListInvalidationsRequest;
import software.amazon.awssdk.services.cloudfront.model.ListInvalidationsResult;
import software.amazon.awssdk.services.cloudfront.model.LoggingConfig;
import software.amazon.awssdk.services.cloudfront.model.Method;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Origins;
import software.amazon.awssdk.services.cloudfront.model.Paths;
import software.amazon.awssdk.services.cloudfront.model.PriceClass;
import software.amazon.awssdk.services.cloudfront.model.Restrictions;
import software.amazon.awssdk.services.cloudfront.model.S3OriginConfig;
import software.amazon.awssdk.services.cloudfront.model.TrustedSigners;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.UpdateDistributionResult;
import software.amazon.awssdk.services.cloudfront.model.ViewerCertificate;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.StringUtils;

/**
 * Integration tests for Cloud Front operations.
 */
public class CloudFrontIntegrationTest extends IntegrationTestBase {

    private static final String CNAME1 = "alias1." + System.currentTimeMillis() + ".example.com";
    private static final String CNAME2 = "alias2." + System.currentTimeMillis() + ".example.com";
    private static final String DEFAULT_ROOT_OBJECT = "index.html";
    private static final String DISTRIBUTION_COMMENT = "comment";
    private static final PriceClass PRICE_CLASS = PriceClass.PriceClass_100;
    private static final ItemSelection FORWARD = ItemSelection.All;
    private static final int CUSTOMIZED_ERROR_CODE = 404;
    private static final String CUSTOMIZED_RESPONSE_PAGE_PATH = "/java_test_404.html";
    private static final String CUSTOMIZED_RESPONSE_CODE = "200";

    private static final String BUCKET_NAME = StringUtils.lowerCase(CloudFrontIntegrationTest.class.getSimpleName()) + "."
                                              + System.currentTimeMillis();

    private static String distributionId;
    private static String distributionETag;
    private static String dnsName;
    private static String callerReference = Long.toString(System.currentTimeMillis());

    /** Release any created test resources. */
    @AfterClass
    public static void tearDown() throws Exception {
        if (distributionId != null) {
            try {
                cloudfront.deleteDistribution(new DeleteDistributionRequest().withId(distributionId).withIfMatch(
                        distributionETag));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    @BeforeClass
    public static void createBucket() throws Exception {
        IntegrationTestBase.setUp();

        s3.createBucket(BUCKET_NAME);
        Thread.sleep(10 * 1000);
        dnsName = BUCKET_NAME + ".s3.amazonaws.com";
        File content = new RandomTempFile("" + System.currentTimeMillis(), 1000L);
        s3.putObject(BUCKET_NAME, "key", content);
    }


    /**
     * Tests that we can work with distributions (create, edit, list, delete,
     * invalidate, etc).
     */
    @Test
    public void testDistributionOperations() throws Exception {

        // Create Distribution
        CreateDistributionResult createDistributionResult = cloudfront
                .createDistribution(new CreateDistributionRequest().withDistributionConfig(
                        new DistributionConfig()
                           .withDefaultCacheBehavior(
                               new DefaultCacheBehavior()
                                   .withMinTTL(100L)
                                   .withTargetOriginId("1")
                                   .withViewerProtocolPolicy("allow-all")
                                   .withSmoothStreaming(false)
                                   .withTrustedSigners(
                                       new TrustedSigners().withEnabled(true).withQuantity(1)
                                                           .withItems("self"))
                                   .withForwardedValues(new ForwardedValues().withQueryString(true)
                                                                             .withCookies(new CookiePreference()
                                                                                                  .withForward(FORWARD)))
                                   .withAllowedMethods(new AllowedMethods()
                                                               .withQuantity(2)
                                                               .withItems(Method.HEAD, Method.GET)))
                           .withAliases(new Aliases().withItems(CNAME1, CNAME2).withQuantity(2))
                           .withLogging(new LoggingConfig().withEnabled(false).withBucket(
                                   BUCKET_NAME).withPrefix("").withIncludeCookies(false))
                           .withCallerReference(callerReference)
                           .withRestrictions(new Restrictions(new GeoRestriction(GeoRestrictionType.Blacklist)
                                                                      .withItems("DE").withQuantity(1)))

                           .withCacheBehaviors(
                               new CacheBehaviors().withQuantity(1).withItems(
                                   new CacheBehavior()
                                       .withMinTTL(100L)
                                       .withTargetOriginId("1")
                                       .withViewerProtocolPolicy("allow-all")
                                       .withTrustedSigners(
                                           new TrustedSigners().withEnabled(true).withQuantity(1)
                                                               .withItems("self"))
                                       .withForwardedValues(new ForwardedValues().withQueryString(true)
                                                                                 .withCookies(new CookiePreference()
                                                                                                      .withForward(FORWARD)))
                                       .withPathPattern("*")
                                       .withAllowedMethods(new AllowedMethods()
                                                                   .withQuantity(7)
                                                                   .withItems(Method.DELETE, Method.GET, Method.HEAD,
                                                                              Method.OPTIONS, Method.PATCH, Method.POST,
                                                                              Method.PUT))))
                           .withComment(DISTRIBUTION_COMMENT)
                           .withDefaultRootObject(DEFAULT_ROOT_OBJECT)
                           .withEnabled(true)
                           .withPriceClass(PRICE_CLASS)
                           .withOrigins(
                               new Origins().withItems(
                                   new Origin().withDomainName(dnsName).withId("1")
                                               .withS3OriginConfig(new S3OriginConfig().withOriginAccessIdentity("")))
                                        .withQuantity(1))
                           .withViewerCertificate(new ViewerCertificate().withCloudFrontDefaultCertificate(true))
                           .withCustomErrorResponses(new CustomErrorResponses()
                                                         .withQuantity(1)
                                                         .withItems(new CustomErrorResponse()
                                                                        .withErrorCode(CUSTOMIZED_ERROR_CODE)
                                                                        .withResponsePagePath(CUSTOMIZED_RESPONSE_PAGE_PATH)
                                                                        .withResponseCode(CUSTOMIZED_RESPONSE_CODE)))));
        distributionETag = createDistributionResult.getETag();
        assertNotNull(createDistributionResult.getETag());
        assertNotNull(createDistributionResult.getLocation());
        distributionId = createDistributionResult.getDistribution().getId();
        assertValidDistribution(createDistributionResult.getDistribution());

        // List Distributions
        ListDistributionsResult listDistributions = cloudfront.listDistributions(new ListDistributionsRequest());
        assertTrue(listDistributions.getDistributionList() != null);
        assertFalse(listDistributions.getDistributionList().getItems().isEmpty());
        assertNotNull(listDistributions.getDistributionList().getItems().get(0).getId());

        // Get Distribution
        GetDistributionResult getDistributionResult = cloudfront.getDistribution(new GetDistributionRequest()
                                                                                         .withId(distributionId));
        assertNotNull(getDistributionResult.getETag());
        assertValidDistribution(getDistributionResult.getDistribution());

        // Get Distribution Config
        GetDistributionConfigResult getDistributionConfigResult = cloudfront
                .getDistributionConfig(new GetDistributionConfigRequest().withId(distributionId));
        assertNotNull(getDistributionConfigResult.getETag());
        assertValidDistributionConfig(getDistributionConfigResult.getDistributionConfig());

        // Create Invalidation
        CreateInvalidationResult createInvalidationResult = cloudfront
                .createInvalidation(new CreateInvalidationRequest().withDistributionId(distributionId)
                                                                   .withInvalidationBatch(
                                                                           new InvalidationBatch()
                                                                                   .withCallerReference(callerReference)
                                                                                   .withPaths(new Paths().withItems("/index.html")
                                                                                                         .withQuantity(1))));
        assertNotNull(createInvalidationResult.getLocation());
        assertValidInvalidation(createInvalidationResult.getInvalidation());

        // List Invalidations
        ListInvalidationsResult listInvalidationsResult = cloudfront.listInvalidations(
                new ListInvalidationsRequest().withDistributionId(distributionId));
        assertTrue(listInvalidationsResult.getInvalidationList().getItems().size() > 0);
        assertNotNull(listInvalidationsResult.getInvalidationList().getItems().get(0).getId());

        // Get Invalidation
        GetInvalidationResult getInvalidationResult = cloudfront.getInvalidation(
                new GetInvalidationRequest().withDistributionId(distributionId)
                                            .withId(createInvalidationResult.getInvalidation().getId()));
        assertValidInvalidation(getInvalidationResult.getInvalidation());

        // Update Distribution (disable the distribution)
        DistributionConfig distributionConfig = createDistributionResult.getDistribution().getDistributionConfig();
        distributionConfig.setEnabled(false);
        UpdateDistributionResult updateDistributionResult = cloudfront
                .updateDistribution(new UpdateDistributionRequest().withId(distributionId)
                                                                   .withIfMatch(distributionETag)
                                                                   .withDistributionConfig(distributionConfig));
        distributionETag = updateDistributionResult.getETag();
        assertNotNull(updateDistributionResult.getETag());
        assertValidDistribution(updateDistributionResult.getDistribution());

        // Delete Distribution
        waitForDistributionToDeploy(distributionId);
        cloudfront.deleteDistribution(new DeleteDistributionRequest().withId(distributionId).withIfMatch(
                distributionETag));
    }

    /** Asserts that the specified invalidation is valid. */
    private void assertValidInvalidation(Invalidation invalidation) {
        assertNotNull(invalidation.getCreateTime());
        assertNotNull(invalidation.getId());
        assertNotNull(invalidation.getStatus());
        assertEquals(callerReference, invalidation.getInvalidationBatch().getCallerReference());
        assertEquals("/index.html", invalidation.getInvalidationBatch().getPaths().getItems().get(0));
    }

    /** Asserts that the specified distribution is valid. */
    private void assertValidDistribution(Distribution distribution) {
        assertNotNull(distribution.getId());
        assertNotNull(distribution.getDomainName());
        assertNotNull(distribution.getLastModifiedTime());
        assertNotNull(distribution.getStatus());
        assertValidDistributionConfig(distribution.getDistributionConfig());
    }

    /** Asserts that the specified distribution config is valid. */
    private void assertValidDistributionConfig(DistributionConfig distributionConfig) {
        assertEquals(callerReference, distributionConfig.getCallerReference());
        assertEquals(2, distributionConfig.getAliases().getQuantity().intValue());
        assertTrue(distributionConfig.getAliases().getItems().contains(CNAME1));
        assertTrue(distributionConfig.getAliases().getItems().contains(CNAME2));
        assertEquals(DISTRIBUTION_COMMENT, distributionConfig.getComment());
        assertEquals(dnsName, distributionConfig.getOrigins().getItems().get(0).getDomainName());
        assertEquals(DEFAULT_ROOT_OBJECT, distributionConfig.getDefaultRootObject());
        assertEquals(PRICE_CLASS.toString(), distributionConfig.getPriceClass());
        assertFalse(distributionConfig.getLogging().getIncludeCookies());
        assertEquals(FORWARD.toString(), distributionConfig.getDefaultCacheBehavior().getForwardedValues()
                                                           .getCookies().getForward());
        assertEquals(FORWARD.toString(), distributionConfig.getCacheBehaviors().getItems().get(0).getForwardedValues()
                                                           .getCookies().getForward());

        GeoRestriction geoRestriction = distributionConfig.getRestrictions().getGeoRestriction();
        assertEquals(1, geoRestriction.getItems().size());
        assertEquals("DE", geoRestriction.getItems().get(0));
        assertEquals(GeoRestrictionType.Blacklist.toString(), geoRestriction.getRestrictionType());
        assertEquals(1, geoRestriction.getQuantity().intValue());

        assertEquals(1, distributionConfig.getCustomErrorResponses().getQuantity().intValue());
        assertEquals(1, distributionConfig.getCustomErrorResponses().getItems().size());
        CustomErrorResponse customErrorResponse = distributionConfig.getCustomErrorResponses().getItems().get(0);
        assertEquals(CUSTOMIZED_ERROR_CODE, customErrorResponse.getErrorCode().intValue());
        assertEquals(CUSTOMIZED_RESPONSE_PAGE_PATH, customErrorResponse.getResponsePagePath());
        assertEquals(CUSTOMIZED_RESPONSE_CODE, customErrorResponse.getResponseCode());
    }
}
