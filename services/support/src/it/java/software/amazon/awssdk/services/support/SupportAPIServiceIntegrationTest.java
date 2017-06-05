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
 * on an "AS IS" BASIS, oUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.services.support.model.AddCommunicationToCaseRequest;
import software.amazon.awssdk.services.support.model.AddCommunicationToCaseResult;
import software.amazon.awssdk.services.support.model.CreateCaseRequest;
import software.amazon.awssdk.services.support.model.CreateCaseResult;
import software.amazon.awssdk.services.support.model.DescribeCasesRequest;
import software.amazon.awssdk.services.support.model.DescribeCasesResult;
import software.amazon.awssdk.services.support.model.DescribeCommunicationsRequest;
import software.amazon.awssdk.services.support.model.DescribeCommunicationsResult;
import software.amazon.awssdk.services.support.model.DescribeServicesRequest;
import software.amazon.awssdk.services.support.model.DescribeServicesResult;
import software.amazon.awssdk.services.support.model.DescribeSeverityLevelsRequest;
import software.amazon.awssdk.services.support.model.DescribeSeverityLevelsResult;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorCheckRefreshStatusesRequest;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorCheckRefreshStatusesResult;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorCheckResultRequest;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorCheckResultResult;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorCheckSummariesRequest;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorCheckSummariesResult;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorChecksRequest;
import software.amazon.awssdk.services.support.model.DescribeTrustedAdvisorChecksResult;
import software.amazon.awssdk.services.support.model.RefreshTrustedAdvisorCheckRequest;
import software.amazon.awssdk.services.support.model.RefreshTrustedAdvisorCheckResult;
import software.amazon.awssdk.services.support.model.ResolveCaseRequest;

public class SupportAPIServiceIntegrationTest extends IntegrationTestBase {

    private static String caseId;
    private final String SUBJECT = "Fake ticket for support API test";
    private final String CATEGORY_CODE = "apis";
    private final String SERVICE_CODE = "amazon-dynamodb";
    private final String COMMUNICATOR_BODY = "where is zach";
    private final String LANGUAGE = "en";
    private final String SEVERIRY_CODE = "low";
    private String checkId;

    @AfterClass
    public static void teardown() {
        try {
            support.resolveCase(ResolveCaseRequest.builder().caseId(caseId).build());
        } catch (Exception e) {
            // Ignored or expected.
        }
    }


    @Test
    public void testServiceOperations() {
        // Create case
        CreateCaseResult createCaseResult = support.createCase(CreateCaseRequest.builder()
                                                                                .subject(SUBJECT)
                                                                                .categoryCode(CATEGORY_CODE)
                                                                                .serviceCode(SERVICE_CODE)
                                                                                .language(LANGUAGE)
                                                                                .severityCode(SEVERIRY_CODE)
                                                                                .communicationBody(COMMUNICATOR_BODY)
                                                                                .build());


        caseId = createCaseResult.caseId();
        assertNotNull(caseId);

        // Describe cases
        DescribeCasesResult describeCasesResult = support.describeCases(DescribeCasesRequest.builder().build());
        assertTrue(describeCasesResult.cases().size() > 0);

        // Describe case with case Id
        describeCasesResult = support.describeCases(DescribeCasesRequest.builder().caseIdList(caseId).build());
        assertEquals(1, describeCasesResult.cases().size());
        assertEquals(caseId, describeCasesResult.cases().get(0).caseId());
        assertEquals(CATEGORY_CODE, describeCasesResult.cases().get(0).categoryCode());
        assertEquals(LANGUAGE, describeCasesResult.cases().get(0).language());
        assertEquals(SERVICE_CODE, describeCasesResult.cases().get(0).serviceCode());
        assertEquals(SEVERIRY_CODE, describeCasesResult.cases().get(0).severityCode());
        assertTrue(describeCasesResult.cases().get(0).recentCommunications().communications().size() > 0);

        // Describe services
        DescribeServicesResult describeServicesResult = support.describeServices(DescribeServicesRequest.builder().build());
        assertTrue(describeServicesResult.services().size() > 0);
        assertNotNull(describeServicesResult.services().get(0).code());
        assertNotNull(describeServicesResult.services().get(0).name());
        assertTrue(describeServicesResult.services().get(0).categories().size() > 0);
        assertNotNull(describeServicesResult.services().get(0).categories().get(0).code());
        assertNotNull(describeServicesResult.services().get(0).categories().get(0).name());

        // Describe service with service code
        describeServicesResult = support.describeServices(DescribeServicesRequest.builder().serviceCodeList(SERVICE_CODE).language(LANGUAGE).build());
        assertEquals(1, describeServicesResult.services().size());
        assertNotNull(describeServicesResult.services().get(0).name());
        assertEquals(SERVICE_CODE, describeServicesResult.services().get(0).code());

        // Add communication
        AddCommunicationToCaseResult addCommunicationToCaseResult =
                support.addCommunicationToCase(AddCommunicationToCaseRequest.builder().caseId(caseId).communicationBody(COMMUNICATOR_BODY).build());
        assertTrue(addCommunicationToCaseResult.result());

        // Describe communication
        DescribeCommunicationsResult describeCommunicationsResult =
                support.describeCommunications(DescribeCommunicationsRequest.builder().caseId(caseId).build());
        assertTrue(describeCommunicationsResult.communications().size() > 0);
        assertEquals(caseId, describeCommunicationsResult.communications().get(0).caseId());
        assertEquals(COMMUNICATOR_BODY.trim(), describeCommunicationsResult.communications().get(0).body().trim());
        assertNotNull(describeCommunicationsResult.communications().get(0).submittedBy());
        assertNotNull(describeCommunicationsResult.communications().get(0).timeCreated());

        // Describe severity levels
        DescribeSeverityLevelsResult describeSeverityLevelResult =
                support.describeSeverityLevels(DescribeSeverityLevelsRequest.builder().build());
        assertTrue(describeSeverityLevelResult.severityLevels().size() > 0);
        assertNotNull(describeSeverityLevelResult.severityLevels().get(0).name());
        assertNotNull(describeSeverityLevelResult.severityLevels().get(0).code());

        //  Describe trusted advisor checks
        DescribeTrustedAdvisorChecksResult describeTrustedAdvisorChecksResult =
                support.describeTrustedAdvisorChecks(DescribeTrustedAdvisorChecksRequest.builder().language(LANGUAGE).build());
        assertNotNull(describeTrustedAdvisorChecksResult.checks().size() > 0);
        checkId = describeTrustedAdvisorChecksResult.checks().get(0).id();
        assertNotNull(checkId);
        assertNotNull(describeTrustedAdvisorChecksResult.checks().get(0).name());
        assertNotNull(describeTrustedAdvisorChecksResult.checks().get(0).category());
        assertNotNull(describeTrustedAdvisorChecksResult.checks().get(0).description());
        assertTrue(describeTrustedAdvisorChecksResult.checks().get(0).metadata().size() > 0);
        assertNotNull(describeTrustedAdvisorChecksResult.checks().get(0).metadata().get(0));

        // Describe advisor check refresh status
        DescribeTrustedAdvisorCheckRefreshStatusesResult describeTrustedAdvisorCheckRefreshStatusesResult =
                support.describeTrustedAdvisorCheckRefreshStatuses(DescribeTrustedAdvisorCheckRefreshStatusesRequest.builder().checkIds(checkId).build());
        assertNotNull(describeTrustedAdvisorCheckRefreshStatusesResult.statuses());
        assertEquals(1, describeTrustedAdvisorCheckRefreshStatusesResult.statuses().size());
        assertEquals(checkId, describeTrustedAdvisorCheckRefreshStatusesResult.statuses().get(0).checkId());
        assertNotNull(describeTrustedAdvisorCheckRefreshStatusesResult.statuses().get(0).status());
        assertNotNull(describeTrustedAdvisorCheckRefreshStatusesResult.statuses().get(0).millisUntilNextRefreshable());

        // Refresh trusted advisor check
        RefreshTrustedAdvisorCheckResult refreshTrustedAdvisorCheckResult =
                support.refreshTrustedAdvisorCheck(RefreshTrustedAdvisorCheckRequest.builder().checkId(checkId).build());
        assertNotNull(refreshTrustedAdvisorCheckResult.status());

        // Describe trusted advisor check summaries
        DescribeTrustedAdvisorCheckSummariesResult describeTrustedAdvisorCheckSummariesResult =
                support.describeTrustedAdvisorCheckSummaries(DescribeTrustedAdvisorCheckSummariesRequest.builder().checkIds(checkId).build());
        assertEquals(1, describeTrustedAdvisorCheckSummariesResult.summaries().size());
        assertEquals(checkId, describeTrustedAdvisorCheckSummariesResult.summaries().get(0).checkId());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.summaries().get(0).status());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.summaries().get(0).timestamp());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.summaries().get(0).resourcesSummary());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.summaries().get(0).categorySpecificSummary());

        // Describe trusted advisor check result
        DescribeTrustedAdvisorCheckResultResult describeTrustedAdvisorCheckResultResult =
                support.describeTrustedAdvisorCheckResult(DescribeTrustedAdvisorCheckResultRequest.builder().checkId(checkId).build());
        assertNotNull(describeTrustedAdvisorCheckResultResult.result().timestamp());
        assertNotNull(describeTrustedAdvisorCheckResultResult.result().status());
        assertNotNull(describeTrustedAdvisorCheckResultResult.result().resourcesSummary());

    }

}
