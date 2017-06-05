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

    private static final String BUCKET_NAME =
            StringUtils.lowerCase(CloudFrontIntegrationTest.class.getSimpleName()) + "." + System.currentTimeMillis();

    private static String distributionId;
    private static String distributionETag;
    private static String dnsName;
    private static String callerReference = Long.toString(System.currentTimeMillis());

    /** Release any created test resources. */
    @AfterClass
    public static void tearDown() throws Exception {
        if (distributionId != null) {
            try {
                cloudfront.deleteDistribution(
                        DeleteDistributionRequest.builder().id(distributionId).ifMatch(distributionETag).build());
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
        CreateDistributionRequest request = CreateDistributionRequest.builder()
            .distributionConfig(DistributionConfig.builder()
                                    .defaultCacheBehavior(DefaultCacheBehavior.builder()
                                                              .minTTL(100L)
                                                              .targetOriginId("1")
                                                              .viewerProtocolPolicy("allow-all")
                                                              .smoothStreaming(false)
                                                              .trustedSigners(TrustedSigners.builder()
                                                                                  .enabled(true)
                                                                                  .quantity(1)
                                                                                  .items("self")
                                                                                  .build())
                                                              .forwardedValues(ForwardedValues.builder()
                                                                                   .queryString(true)
                                                                                   .cookies(CookiePreference.builder()
                                                                                                .forward(FORWARD)
                                                                                                .build())
                                                                                   .build())
                                                              .allowedMethods(AllowedMethods.builder()
                                                                                  .quantity(2)
                                                                                  .items(Method.HEAD, Method.GET)
                                                                                  .build())
                                                              .build())
                                    .aliases(Aliases.builder()
                                                 .items(CNAME1, CNAME2)
                                                 .quantity(2)
                                                 .build())
                                    .logging(LoggingConfig.builder()
                                                 .enabled(false)
                                                 .bucket(BUCKET_NAME)
                                                 .prefix("")
                                                 .includeCookies(false)
                                                 .build())
                                    .callerReference(callerReference)
                                    .restrictions(Restrictions.builder()
                                                      .geoRestriction(GeoRestriction.builder()
                                                                          .restrictionType(GeoRestrictionType.Blacklist)
                                                                          .items("DE")
                                                                          .quantity(1)
                                                                          .build())
                                                      .build())
                                    .cacheBehaviors(CacheBehaviors.builder()
                                                        .quantity(1)
                                                        .items(CacheBehavior.builder()
                                                                   .minTTL(100L)
                                                                   .targetOriginId("1")
                                                                   .viewerProtocolPolicy("allow-all")
                                                                   .trustedSigners(TrustedSigners.builder()
                                                                                       .enabled(true)
                                                                                       .quantity(1)
                                                                                       .items("self")
                                                                                       .build())
                                                                   .forwardedValues(ForwardedValues.builder()
                                                                                        .queryString(true)
                                                                                        .cookies(CookiePreference.builder()
                                                                                                     .forward(FORWARD)
                                                                                                     .build())
                                                                                        .build())
                                                                   .pathPattern("*")
                                                                   .allowedMethods(AllowedMethods.builder()
                                                                                       .quantity(7)
                                                                                       .items(Method.DELETE, Method.GET,
                                                                                              Method.HEAD, Method.OPTIONS,
                                                                                              Method.PATCH, Method.POST,
                                                                                              Method.PUT)
                                                                                       .build())
                                                                   .build())
                                                        .build())
                                    .comment(DISTRIBUTION_COMMENT)
                                    .defaultRootObject(DEFAULT_ROOT_OBJECT)
                                    .enabled(true)
                                    .priceClass(PRICE_CLASS)
                                    .origins(Origins.builder()
                                                 .items(Origin.builder()
                                                            .domainName(dnsName)
                                                            .id("1")
                                                            .s3OriginConfig(S3OriginConfig.builder()
                                                                                .originAccessIdentity("")
                                                                                .build())
                                                            .build())
                                                 .quantity(1)
                                                 .build())
                                    .viewerCertificate(ViewerCertificate.builder()
                                                           .cloudFrontDefaultCertificate(true)
                                                           .build())
                                    .customErrorResponses(CustomErrorResponses.builder()
                                                              .quantity(1)
                                                              .items(CustomErrorResponse.builder()
                                                                         .errorCode(CUSTOMIZED_ERROR_CODE)
                                                                         .responsePagePath(CUSTOMIZED_RESPONSE_PAGE_PATH)
                                                                         .responseCode(CUSTOMIZED_RESPONSE_CODE)
                                                                         .build())
                                                              .build())
                                    .build())
            .build();
        CreateDistributionResult createDistributionResult = cloudfront.createDistribution(request);
        distributionETag = createDistributionResult.eTag();
        assertNotNull(createDistributionResult.eTag());
        assertNotNull(createDistributionResult.location());
        distributionId = createDistributionResult.distribution().id();
        assertValidDistribution(createDistributionResult.distribution());

        // List Distributions
        ListDistributionsResult listDistributions = cloudfront.listDistributions(ListDistributionsRequest.builder().build());
        assertTrue(listDistributions.distributionList() != null);
        assertFalse(listDistributions.distributionList().items().isEmpty());
        assertNotNull(listDistributions.distributionList().items().get(0).id());

        // Get Distribution
        GetDistributionResult getDistributionResult =
                cloudfront.getDistribution(GetDistributionRequest.builder().id(distributionId).build());
        assertNotNull(getDistributionResult.eTag());
        assertValidDistribution(getDistributionResult.distribution());

        // Get Distribution Config
        GetDistributionConfigResult getDistributionConfigResult =
                cloudfront.getDistributionConfig(GetDistributionConfigRequest.builder().id(distributionId).build());
        assertNotNull(getDistributionConfigResult.eTag());
        assertValidDistributionConfig(getDistributionConfigResult.distributionConfig());

        // Create Invalidation
        CreateInvalidationResult createInvalidationResult = cloudfront.createInvalidation(
                CreateInvalidationRequest.builder().distributionId(distributionId).invalidationBatch(
                        InvalidationBatch.builder().callerReference(callerReference)
                                .paths(Paths.builder().items("/index.html").quantity(1).build()).build()).build());
        assertNotNull(createInvalidationResult.location());
        assertValidInvalidation(createInvalidationResult.invalidation());

        // List Invalidations
        ListInvalidationsResult listInvalidationsResult =
                cloudfront.listInvalidations(ListInvalidationsRequest.builder().distributionId(distributionId).build());
        assertTrue(listInvalidationsResult.invalidationList().items().size() > 0);
        assertNotNull(listInvalidationsResult.invalidationList().items().get(0).id());

        // Get Invalidation
        GetInvalidationResult getInvalidationResult = cloudfront.getInvalidation(
                GetInvalidationRequest.builder().distributionId(distributionId).id(createInvalidationResult.invalidation().id())
                        .build());
        assertValidInvalidation(getInvalidationResult.invalidation());

        // Update Distribution (disable the distribution)
        DistributionConfig distributionConfig = createDistributionResult.distribution()
                                                                        .distributionConfig()
                                                                        .toBuilder()
                                                                        .enabled(true)
                                                                        .build();
        UpdateDistributionResult updateDistributionResult = cloudfront.updateDistribution(
                UpdateDistributionRequest.builder().id(distributionId).ifMatch(distributionETag)
                        .distributionConfig(distributionConfig).build());
        distributionETag = updateDistributionResult.eTag();
        assertNotNull(updateDistributionResult.eTag());
        assertValidDistribution(updateDistributionResult.distribution());

        // Delete Distribution
        waitForDistributionToDeploy(distributionId);
        cloudfront.deleteDistribution(DeleteDistributionRequest.builder().id(distributionId).ifMatch(distributionETag).build());
    }

    /** Asserts that the specified invalidation is valid. */
    private void assertValidInvalidation(Invalidation invalidation) {
        assertNotNull(invalidation.createTime());
        assertNotNull(invalidation.id());
        assertNotNull(invalidation.status());
        assertEquals(callerReference, invalidation.invalidationBatch().callerReference());
        assertEquals("/index.html", invalidation.invalidationBatch().paths().items().get(0));
    }

    /** Asserts that the specified distribution is valid. */
    private void assertValidDistribution(Distribution distribution) {
        assertNotNull(distribution.id());
        assertNotNull(distribution.domainName());
        assertNotNull(distribution.lastModifiedTime());
        assertNotNull(distribution.status());
        assertValidDistributionConfig(distribution.distributionConfig());
    }

    /** Asserts that the specified distribution config is valid. */
    private void assertValidDistributionConfig(DistributionConfig distributionConfig) {
        assertEquals(callerReference, distributionConfig.callerReference());
        assertEquals(2, distributionConfig.aliases().quantity().intValue());
        assertTrue(distributionConfig.aliases().items().contains(CNAME1));
        assertTrue(distributionConfig.aliases().items().contains(CNAME2));
        assertEquals(DISTRIBUTION_COMMENT, distributionConfig.comment());
        assertEquals(dnsName, distributionConfig.origins().items().get(0).domainName());
        assertEquals(DEFAULT_ROOT_OBJECT, distributionConfig.defaultRootObject());
        assertEquals(PRICE_CLASS.toString(), distributionConfig.priceClass());
        assertFalse(distributionConfig.logging().includeCookies());
        assertEquals(FORWARD.toString(), distributionConfig.defaultCacheBehavior().forwardedValues().cookies().forward());
        assertEquals(FORWARD.toString(),
                     distributionConfig.cacheBehaviors().items().get(0).forwardedValues().cookies().forward());

        GeoRestriction geoRestriction = distributionConfig.restrictions().geoRestriction();
        assertEquals(1, geoRestriction.items().size());
        assertEquals("DE", geoRestriction.items().get(0));
        assertEquals(GeoRestrictionType.Blacklist.toString(), geoRestriction.restrictionType());
        assertEquals(1, geoRestriction.quantity().intValue());

        assertEquals(1, distributionConfig.customErrorResponses().quantity().intValue());
        assertEquals(1, distributionConfig.customErrorResponses().items().size());
        CustomErrorResponse customErrorResponse = distributionConfig.customErrorResponses().items().get(0);
        assertEquals(CUSTOMIZED_ERROR_CODE, customErrorResponse.errorCode().intValue());
        assertEquals(CUSTOMIZED_RESPONSE_PAGE_PATH, customErrorResponse.responsePagePath());
        assertEquals(CUSTOMIZED_RESPONSE_CODE, customErrorResponse.responseCode());
    }
}
