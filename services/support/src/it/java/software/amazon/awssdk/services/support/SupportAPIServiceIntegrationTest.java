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

    private final String SUBJECT = "Fake ticket for support API test";
    private final String CATEGORY_CODE = "apis";
    private final String SERVICE_CODE = "amazon-dynamodb";
    private final String COMMUNICATOR_BODY = "where is zach";
    private final String LANGUAGE = "en";
    private final String SEVERIRY_CODE = "low";
    private static String caseId;
    private String checkId;

    @AfterClass
    public static void teardown() {
        try {
        support.resolveCase(new ResolveCaseRequest().withCaseId(caseId));
        } catch (Exception e) {

        }
    }


    @Test
    public void testServiceOperations() {
        // Create case
        CreateCaseResult createCaseResult = support.createCase(new CreateCaseRequest()
           .withSubject(SUBJECT)
           .withCategoryCode(CATEGORY_CODE)
           .withServiceCode(SERVICE_CODE)
           .withLanguage(LANGUAGE)
           .withSeverityCode(SEVERIRY_CODE)
           .withCommunicationBody(COMMUNICATOR_BODY));


        caseId = createCaseResult.getCaseId();
        assertNotNull(caseId);

        // Describe cases
        DescribeCasesResult describeCasesResult = support.describeCases();
        assertTrue(describeCasesResult.getCases().size() > 0);

        // Describe case with case Id
        describeCasesResult = support.describeCases(new DescribeCasesRequest().withCaseIdList(caseId));
        assertEquals(1, describeCasesResult.getCases().size());
        assertEquals(caseId, describeCasesResult.getCases().get(0).getCaseId());
        assertEquals(CATEGORY_CODE, describeCasesResult.getCases().get(0).getCategoryCode());
        assertEquals(LANGUAGE, describeCasesResult.getCases().get(0).getLanguage());
        assertEquals(SERVICE_CODE, describeCasesResult.getCases().get(0).getServiceCode());
        assertEquals(SEVERIRY_CODE, describeCasesResult.getCases().get(0).getSeverityCode());
        assertTrue(describeCasesResult.getCases().get(0).getRecentCommunications().getCommunications().size() > 0);

        // Describe services
        DescribeServicesResult describeServicesResult = support.describeServices();
        assertTrue(describeServicesResult.getServices().size() > 0);
        assertNotNull(describeServicesResult.getServices().get(0).getCode());
        assertNotNull(describeServicesResult.getServices().get(0).getName());
        assertTrue(describeServicesResult.getServices().get(0).getCategories().size() > 0);
        assertNotNull(describeServicesResult.getServices().get(0).getCategories().get(0).getCode());
        assertNotNull(describeServicesResult.getServices().get(0).getCategories().get(0).getName());

        // Describe service with service code
        describeServicesResult = support.describeServices(new DescribeServicesRequest().withServiceCodeList(SERVICE_CODE).withLanguage(LANGUAGE));
        assertEquals(1, describeServicesResult.getServices().size());
        assertNotNull(describeServicesResult.getServices().get(0).getName());
        assertEquals(SERVICE_CODE, describeServicesResult.getServices().get(0).getCode());

        // Add communication
        AddCommunicationToCaseResult  addCommunicationToCaseResult = support.addCommunicationToCase(new AddCommunicationToCaseRequest().withCaseId(caseId).withCommunicationBody(COMMUNICATOR_BODY));
        assertTrue(addCommunicationToCaseResult.getResult());

        // Describe communication
        DescribeCommunicationsResult describeCommunicationsResult = support.describeCommunications(new DescribeCommunicationsRequest().withCaseId(caseId));
        assertTrue(describeCommunicationsResult.getCommunications().size() > 0);
        assertEquals(caseId, describeCommunicationsResult.getCommunications().get(0).getCaseId());
        assertEquals(COMMUNICATOR_BODY.trim(), describeCommunicationsResult.getCommunications().get(0).getBody().trim());
        assertNotNull(describeCommunicationsResult.getCommunications().get(0).getSubmittedBy());
        assertNotNull(describeCommunicationsResult.getCommunications().get(0).getTimeCreated());

        // Describe severity levels
        DescribeSeverityLevelsResult describeSeverityLevelResult = support.describeSeverityLevels();
        assertTrue(describeSeverityLevelResult.getSeverityLevels().size() > 0);
        assertNotNull(describeSeverityLevelResult.getSeverityLevels().get(0).getName());
        assertNotNull(describeSeverityLevelResult.getSeverityLevels().get(0).getCode());

        //  Describe trusted advisor checks
        DescribeTrustedAdvisorChecksResult describeTrustedAdvisorChecksResult = support.describeTrustedAdvisorChecks(new DescribeTrustedAdvisorChecksRequest().withLanguage(LANGUAGE));
        assertNotNull(describeTrustedAdvisorChecksResult.getChecks().size() > 0);
        checkId = describeTrustedAdvisorChecksResult.getChecks().get(0).getId();
        assertNotNull(checkId);
        assertNotNull(describeTrustedAdvisorChecksResult.getChecks().get(0).getName());
        assertNotNull(describeTrustedAdvisorChecksResult.getChecks().get(0).getCategory());
        assertNotNull(describeTrustedAdvisorChecksResult.getChecks().get(0).getDescription());
        assertTrue(describeTrustedAdvisorChecksResult.getChecks().get(0).getMetadata().size() > 0);
        assertNotNull(describeTrustedAdvisorChecksResult.getChecks().get(0).getMetadata().get(0));

        // Describe advisor check refresh status
        DescribeTrustedAdvisorCheckRefreshStatusesResult describeTrustedAdvisorCheckRefreshStatusesResult =
                   support.describeTrustedAdvisorCheckRefreshStatuses(new DescribeTrustedAdvisorCheckRefreshStatusesRequest()
                                                                      .withCheckIds(checkId));
        assertNotNull(describeTrustedAdvisorCheckRefreshStatusesResult.getStatuses());
        assertEquals(1, describeTrustedAdvisorCheckRefreshStatusesResult.getStatuses().size());
        assertEquals(checkId, describeTrustedAdvisorCheckRefreshStatusesResult.getStatuses().get(0).getCheckId());
        assertNotNull(describeTrustedAdvisorCheckRefreshStatusesResult.getStatuses().get(0).getStatus());
        assertNotNull(describeTrustedAdvisorCheckRefreshStatusesResult.getStatuses().get(0).getMillisUntilNextRefreshable());

        // Refresh trusted advisor check
        RefreshTrustedAdvisorCheckResult refreshTrustedAdvisorCheckResult = support.refreshTrustedAdvisorCheck(new RefreshTrustedAdvisorCheckRequest().withCheckId(checkId));
        assertNotNull(refreshTrustedAdvisorCheckResult.getStatus());

        // Describe trusted advisor check summaries
        DescribeTrustedAdvisorCheckSummariesResult describeTrustedAdvisorCheckSummariesResult = support.describeTrustedAdvisorCheckSummaries(new DescribeTrustedAdvisorCheckSummariesRequest().withCheckIds(checkId));
        assertEquals(1, describeTrustedAdvisorCheckSummariesResult.getSummaries().size());
        assertEquals(checkId, describeTrustedAdvisorCheckSummariesResult.getSummaries().get(0).getCheckId());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.getSummaries().get(0).getStatus());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.getSummaries().get(0).getTimestamp());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.getSummaries().get(0).getResourcesSummary());
        assertNotNull(describeTrustedAdvisorCheckSummariesResult.getSummaries().get(0).getCategorySpecificSummary());

        // Describe trusted advisor check result
        DescribeTrustedAdvisorCheckResultResult describeTrustedAdvisorCheckResultResult = support.describeTrustedAdvisorCheckResult(new DescribeTrustedAdvisorCheckResultRequest().withCheckId(checkId));
        assertNotNull(describeTrustedAdvisorCheckResultResult.getResult().getTimestamp());
        assertNotNull(describeTrustedAdvisorCheckResultResult.getResult().getStatus());
        assertNotNull(describeTrustedAdvisorCheckResultResult.getResult().getResourcesSummary());

    }

}
