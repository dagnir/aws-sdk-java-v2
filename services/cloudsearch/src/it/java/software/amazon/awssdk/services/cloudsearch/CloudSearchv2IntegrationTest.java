package software.amazon.awssdk.services.cloudsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.services.cloudsearch.model.AccessPoliciesStatus;
import software.amazon.awssdk.services.cloudsearch.model.AnalysisScheme;
import software.amazon.awssdk.services.cloudsearch.model.AnalysisSchemeLanguage;
import software.amazon.awssdk.services.cloudsearch.model.AnalysisSchemeStatus;
import software.amazon.awssdk.services.cloudsearch.model.BuildSuggestersRequest;
import software.amazon.awssdk.services.cloudsearch.model.CreateDomainRequest;
import software.amazon.awssdk.services.cloudsearch.model.CreateDomainResult;
import software.amazon.awssdk.services.cloudsearch.model.DefineAnalysisSchemeRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineExpressionRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineIndexFieldRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineSuggesterRequest;
import software.amazon.awssdk.services.cloudsearch.model.DefineSuggesterResult;
import software.amazon.awssdk.services.cloudsearch.model.DeleteDomainRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeAnalysisSchemesRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeAnalysisSchemesResult;
import software.amazon.awssdk.services.cloudsearch.model.DescribeDomainsRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeDomainsResult;
import software.amazon.awssdk.services.cloudsearch.model.DescribeExpressionsRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeExpressionsResult;
import software.amazon.awssdk.services.cloudsearch.model.DescribeIndexFieldsRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeIndexFieldsResult;
import software.amazon.awssdk.services.cloudsearch.model.DescribeScalingParametersRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeScalingParametersResult;
import software.amazon.awssdk.services.cloudsearch.model.DescribeServiceAccessPoliciesRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeServiceAccessPoliciesResult;
import software.amazon.awssdk.services.cloudsearch.model.DescribeSuggestersRequest;
import software.amazon.awssdk.services.cloudsearch.model.DescribeSuggestersResult;
import software.amazon.awssdk.services.cloudsearch.model.DocumentSuggesterOptions;
import software.amazon.awssdk.services.cloudsearch.model.DomainStatus;
import software.amazon.awssdk.services.cloudsearch.model.Expression;
import software.amazon.awssdk.services.cloudsearch.model.ExpressionStatus;
import software.amazon.awssdk.services.cloudsearch.model.IndexDocumentsRequest;
import software.amazon.awssdk.services.cloudsearch.model.IndexField;
import software.amazon.awssdk.services.cloudsearch.model.IndexFieldStatus;
import software.amazon.awssdk.services.cloudsearch.model.IndexFieldType;
import software.amazon.awssdk.services.cloudsearch.model.ListDomainNamesRequest;
import software.amazon.awssdk.services.cloudsearch.model.ListDomainNamesResult;
import software.amazon.awssdk.services.cloudsearch.model.PartitionInstanceType;
import software.amazon.awssdk.services.cloudsearch.model.ScalingParameters;
import software.amazon.awssdk.services.cloudsearch.model.Suggester;
import software.amazon.awssdk.services.cloudsearch.model.SuggesterStatus;
import software.amazon.awssdk.services.cloudsearch.model.TextOptions;
import software.amazon.awssdk.services.cloudsearch.model.UpdateScalingParametersRequest;
import software.amazon.awssdk.services.cloudsearch.model.UpdateServiceAccessPoliciesRequest;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class CloudSearchv2IntegrationTest extends AwsIntegrationTestBase {

    /** Name Prefix of the domains being created for test cases. */
    private static final String testDomainNamePrefix = "sdk-domain-";
    /** Name of the expression being created in the domain. */
    private static final String testExpressionName = "sdkexp"
                                                     + System.currentTimeMillis();
    /** Name of the test index being created in the domain. */
    private static final String testIndexName = "sdkindex"
                                                + System.currentTimeMillis();
    /** Name of the test suggester being created in the domain. */
    private static final String testSuggesterName = "sdksug"
                                                    + System.currentTimeMillis();
    /** Name of the test analysis scheme being created in the domain. */
    private static final String testAnalysisSchemeName = "analysis"
                                                         + System.currentTimeMillis();
    public static String POLICY = "{\"Statement\": " + "[ "
                                  + "{\"Effect\":\"Allow\", " + "\"Action\":\"*\", "
                                  + "\"Condition\": " + "{ " + "\"IpAddress\": "
                                  + "{ \"aws:SourceIp\": [\"203.0.113.1/32\"]} "
                                  + "} " + "}" + "] " + "}";
    /** Reference to the cloud search client during the testing process. */
    private static CloudSearchClient cloudSearch = null;
    /**
     * Holds the name of the domain name at any point of time during test case
     * execution.
     */
    private static String testDomainName = null;

    /**
     * Sets up the credenitals and creates an instance of the Amazon Cloud
     * Search client used for different test case executions.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        cloudSearch = CloudSearchClient.builder().withCredentials(new AwsStaticCredentialsProvider(getCredentials())).build();
    }

    /**
     * Creates a new Amazon Cloud Search domain before every test case
     * execution. This is done to ensure that the state of domain is in
     * consistent state before and after the test case execution.
     */
    @Before
    public void createDomain() {

        testDomainName = testDomainNamePrefix + System.currentTimeMillis();

        cloudSearch.createDomain(new CreateDomainRequest()
                                         .withDomainName(testDomainName));
    }

    /**
     * Deletes the Amazon Cloud Search domain after every test case execution.
     */
    @After
    public void deleteDomain() {
        cloudSearch.deleteDomain(new DeleteDomainRequest()
                                         .withDomainName(testDomainName));
    }

    /**
     * Tests the create domain functionality. Checks if there are any existing
     * domains by querying using describe domains or list domain names API.
     * Creates a new domain using create domain API. Checks if the domain id,
     * name is set in the result and the domain name matches the name used
     * during creation. Also checks if the state of the domain in Created State.
     * Since this domain is created locally for this test case, it is deleted in
     * the finally block.
     */
    @Test
    public void testCreateDomains() {

        CreateDomainResult createDomainResult = null;
        String domainName = "test-" + System.currentTimeMillis();
        try {
            DescribeDomainsResult describeDomainResult = cloudSearch
                    .describeDomains(new DescribeDomainsRequest());
            ListDomainNamesResult listDomainNamesResult = cloudSearch
                    .listDomainNames(new ListDomainNamesRequest());

            assertTrue(describeDomainResult.getDomainStatusList().size() >= 0);
            assertTrue(listDomainNamesResult.getDomainNames().size() >= 0);

            createDomainResult = cloudSearch
                    .createDomain(new CreateDomainRequest()
                                          .withDomainName(domainName));

            describeDomainResult = cloudSearch
                    .describeDomains(new DescribeDomainsRequest()
                                             .withDomainNames(domainName));
            DomainStatus domainStatus = describeDomainResult
                    .getDomainStatusList().get(0);

            assertTrue(domainStatus.getCreated());
            assertFalse(domainStatus.getDeleted());
            assertNotNull(domainStatus.getARN());
            assertEquals(domainStatus.getDomainName(), domainName);
            assertNotNull(domainStatus.getDomainId());
            assertTrue(domainStatus.getProcessing());
        } finally {
            if (createDomainResult != null) {
                cloudSearch.deleteDomain(new DeleteDomainRequest()
                                                 .withDomainName(domainName));
            }
        }

    }

    /**
     * Tests the Index Documents API. Asserts that the status of the domain is
     * initially in the "RequiresIndexDocuments" state. After an index document
     * request is initiated, the status must be updated to "Processing" state.
     * Status is retrieved using the Describe Domains API
     */
    @Test
    public void testIndexDocuments() {

        IndexField indexField = new IndexField().withIndexFieldName(
                testIndexName).withIndexFieldType(IndexFieldType.Literal);

        cloudSearch.defineIndexField(new DefineIndexFieldRequest()
                                             .withDomainName(testDomainName).withIndexField(indexField));
        DescribeDomainsResult describeDomainResult = cloudSearch
                .describeDomains(new DescribeDomainsRequest()
                                         .withDomainNames(testDomainName));
        DomainStatus status = describeDomainResult.getDomainStatusList().get(0);
        assertTrue(status.getRequiresIndexDocuments());

        cloudSearch.indexDocuments(new IndexDocumentsRequest()
                                           .withDomainName(testDomainName));
        status = describeDomainResult.getDomainStatusList().get(0);
        assertTrue(status.getProcessing());

    }

    /**
     * Tests the Access Policies API. Updates an Access Policy for the domain.
     * Retrieves the access policy and checks if the access policy retrieved is
     * same as the one updated.
     */
    @Test
    public void testAccessPolicies() {

        AccessPoliciesStatus accessPoliciesStatus = null;
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        DescribeDomainsResult describeDomainResult = cloudSearch
                .describeDomains(new DescribeDomainsRequest()
                                         .withDomainNames(testDomainName));

        POLICY = POLICY.replaceAll("ARN", describeDomainResult
                .getDomainStatusList().get(0).getARN());
        cloudSearch
                .updateServiceAccessPolicies(new UpdateServiceAccessPoliciesRequest()
                                                     .withDomainName(testDomainName).withAccessPolicies(
                                POLICY));
        DescribeServiceAccessPoliciesResult accessPolicyResult = cloudSearch
                .describeServiceAccessPolicies(new DescribeServiceAccessPoliciesRequest()
                                                       .withDomainName(testDomainName));
        accessPoliciesStatus = accessPolicyResult.getAccessPolicies();

        assertNotNull(accessPoliciesStatus);
        assertTrue(yesterday.getTime().before(
                accessPoliciesStatus.getStatus().getCreationDate()));
        assertTrue(yesterday.getTime().before(
                accessPoliciesStatus.getStatus().getUpdateDate()));
        assertTrue(accessPoliciesStatus.getOptions().length() > 0);
        assertNotNull(accessPoliciesStatus.getStatus().getState());

    }

    /**
     * Test the Define Index Fields API. Asserts that the list of index fields
     * initially in the domain is ZERO. Creates a new index field for every
     * index field type mentioned in the enum
     * <code>com.amazonaws.services.cloudsearch.model.IndexFieldType<code>.
     *
     * Asserts that the number of index fields created is same as the number of the enum type mentioned.
     */
    @Test
    public void testIndexFields() {

        String indexFieldName = null;
        DescribeIndexFieldsRequest describeIndexFieldRequest = new DescribeIndexFieldsRequest()
                .withDomainName(testDomainName);

        DescribeIndexFieldsResult result = cloudSearch
                .describeIndexFields(describeIndexFieldRequest);

        assertTrue(result.getIndexFields().size() == 0);

        IndexField field = null;
        DefineIndexFieldRequest defineIndexFieldRequest = new DefineIndexFieldRequest()
                .withDomainName(testDomainName);
        for (IndexFieldType type : IndexFieldType.values()) {
            indexFieldName = type.toString();
            indexFieldName = indexFieldName.replaceAll("-", "");
            field = new IndexField().withIndexFieldType(type)
                                    .withIndexFieldName(indexFieldName + "indexfield");
            defineIndexFieldRequest.withIndexField(field);
            cloudSearch.defineIndexField(defineIndexFieldRequest);
        }

        result = cloudSearch.describeIndexFields(describeIndexFieldRequest
                                                         .withDeployed(false));
        List<IndexFieldStatus> indexFieldStatusList = result.getIndexFields();
        assertTrue(indexFieldStatusList.size() == IndexFieldType.values().length);
    }

    /**
     * Tests the Define Expressions API. Asserts that the list of expressions in
     * the domain is ZERO. Creates a new Expression. Asserts that the Describe
     * Expression API returns the Expression created.
     */
    @Test
    public void testExpressions() {
        DescribeExpressionsRequest describeExpressionRequest = new DescribeExpressionsRequest()
                .withDomainName(testDomainName);

        DescribeExpressionsResult describeExpressionResult = cloudSearch
                .describeExpressions(describeExpressionRequest);

        assertTrue(describeExpressionResult.getExpressions().size() == 0);

        Expression expression = new Expression().withExpressionName(
                testExpressionName).withExpressionValue("1");
        cloudSearch.defineExpression(new DefineExpressionRequest()
                                             .withDomainName(testDomainName).withExpression(expression));

        describeExpressionResult = cloudSearch
                .describeExpressions(describeExpressionRequest);
        List<ExpressionStatus> expressionStatus = describeExpressionResult
                .getExpressions();
        assertTrue(expressionStatus.size() == 1);

        Expression expressionRetrieved = expressionStatus.get(0).getOptions();
        assertEquals(expression.getExpressionName(),
                     expressionRetrieved.getExpressionName());
        assertEquals(expression.getExpressionValue(),
                     expressionRetrieved.getExpressionValue());
    }

    /**
     * Tests the Define Suggesters API. Asserts that the number of suggesters is
     * ZERO initially in the domain. Creates a suggester for an text field and
     * asserts if the number of suggesters is 1 after creation. Builds the
     * suggesters into the domain and asserts that the domain status is in
     * "Processing" state.
     */
    @Test
    public void testSuggestors() {
        DescribeSuggestersRequest describeSuggesterRequest = new DescribeSuggestersRequest()
                .withDomainName(testDomainName);
        DescribeSuggestersResult describeSuggesterResult = cloudSearch
                .describeSuggesters(describeSuggesterRequest);

        assertTrue(describeSuggesterResult.getSuggesters().size() == 0);

        DefineIndexFieldRequest defineIndexFieldRequest = new DefineIndexFieldRequest()
                .withDomainName(testDomainName)
                .withIndexField(
                        new IndexField()
                                .withIndexFieldName(testIndexName)
                                .withIndexFieldType(IndexFieldType.Text)
                                .withTextOptions(
                                        new TextOptions()
                                                .withAnalysisScheme("_en_default_")));
        cloudSearch.defineIndexField(defineIndexFieldRequest);

        DocumentSuggesterOptions suggesterOptions = new DocumentSuggesterOptions()
                .withSourceField(testIndexName).withSortExpression("1");
        Suggester suggester = new Suggester().withSuggesterName(
                testSuggesterName).withDocumentSuggesterOptions(
                suggesterOptions);
        DefineSuggesterRequest defineSuggesterRequest = new DefineSuggesterRequest()
                .withDomainName(testDomainName).withSuggester(suggester);
        DefineSuggesterResult defineSuggesterResult = cloudSearch
                .defineSuggester(defineSuggesterRequest);
        SuggesterStatus status = defineSuggesterResult.getSuggester();
        assertNotNull(status);
        assertNotNull(status.getOptions());
        assertEquals(status.getOptions().getSuggesterName(), testSuggesterName);

        describeSuggesterResult = cloudSearch
                .describeSuggesters(describeSuggesterRequest);
        assertTrue(describeSuggesterResult.getSuggesters().size() == 1);

        cloudSearch.buildSuggesters(new BuildSuggestersRequest()
                                            .withDomainName(testDomainName));
        DescribeDomainsResult describeDomainsResult = cloudSearch
                .describeDomains(new DescribeDomainsRequest()
                                         .withDomainNames(testDomainName));
        DomainStatus domainStatus = describeDomainsResult.getDomainStatusList()
                                                         .get(0);
        assertTrue(domainStatus.getProcessing());
    }

    /**
     * Tests the Define Analysis Scheme API. Asserts that the number of analysis
     * scheme in a newly created domain is ZERO. Creates an new analysis scheme
     * for the domain. Creates a new index field and associates the analysis
     * scheme with the field. Asserts that the number of analysis scheme is ONE
     * and the matches the analysis scheme retrieved with the one created. Also
     * asserts if the describe index field API returns the index field that has
     * the analysis scheme linked.
     */
    @Test
    public void testAnalysisSchemes() {
        DescribeAnalysisSchemesRequest describeAnalysisSchemesRequest = new DescribeAnalysisSchemesRequest()
                .withDomainName(testDomainName);
        DescribeAnalysisSchemesResult describeAnalysisSchemesResult = cloudSearch
                .describeAnalysisSchemes(describeAnalysisSchemesRequest);
        assertTrue(describeAnalysisSchemesResult.getAnalysisSchemes().size() == 0);

        AnalysisScheme analysisScheme = new AnalysisScheme()
                .withAnalysisSchemeName(testAnalysisSchemeName)
                .withAnalysisSchemeLanguage(AnalysisSchemeLanguage.Ar);
        cloudSearch.defineAnalysisScheme(new DefineAnalysisSchemeRequest()
                                                 .withDomainName(testDomainName).withAnalysisScheme(
                        analysisScheme));

        IndexField indexField = new IndexField().withIndexFieldName(
                testIndexName).withIndexFieldType(IndexFieldType.Text);
        indexField.setTextOptions(new TextOptions()
                                          .withAnalysisScheme(testAnalysisSchemeName));

        DefineIndexFieldRequest defineIndexFieldRequest = new DefineIndexFieldRequest()
                .withDomainName(testDomainName).withIndexField(indexField);
        cloudSearch.defineIndexField(defineIndexFieldRequest);

        describeAnalysisSchemesResult = cloudSearch
                .describeAnalysisSchemes(describeAnalysisSchemesRequest);
        assertTrue(describeAnalysisSchemesResult.getAnalysisSchemes().size() == 1);

        AnalysisSchemeStatus schemeStatus = describeAnalysisSchemesResult
                .getAnalysisSchemes().get(0);
        assertEquals(schemeStatus.getOptions().getAnalysisSchemeName(),
                     testAnalysisSchemeName);
        assertEquals(schemeStatus.getOptions().getAnalysisSchemeLanguage(),
                     AnalysisSchemeLanguage.Ar.toString());

        DescribeIndexFieldsResult describeIndexFieldsResult = cloudSearch
                .describeIndexFields(new DescribeIndexFieldsRequest()
                                             .withDomainName(testDomainName).withFieldNames(
                                testIndexName));
        IndexFieldStatus status = describeIndexFieldsResult.getIndexFields()
                                                           .get(0);
        TextOptions textOptions = status.getOptions().getTextOptions();
        assertEquals(textOptions.getAnalysisScheme(), testAnalysisSchemeName);

    }

    /**
     * Tests the Scaling Parameters API. Updates the scaling parameters for the
     * domain. Retrieves the scaling parameters and checks if it is the same.
     */
    @Test
    public void testScalingParameters() {
        ScalingParameters scalingParameters = new ScalingParameters()
                .withDesiredInstanceType(PartitionInstanceType.SearchM1Small)
                .withDesiredReplicationCount(5)
                .withDesiredPartitionCount(5);
        cloudSearch
                .updateScalingParameters(new UpdateScalingParametersRequest()
                                                 .withDomainName(testDomainName).withScalingParameters(
                                scalingParameters));

        DescribeScalingParametersResult describeScalingParametersResult = cloudSearch
                .describeScalingParameters(new DescribeScalingParametersRequest()
                                                   .withDomainName(testDomainName));
        ScalingParameters retrievedScalingParameters = describeScalingParametersResult
                .getScalingParameters().getOptions();
        assertEquals(retrievedScalingParameters.getDesiredInstanceType(),
                     scalingParameters.getDesiredInstanceType());
        assertEquals(retrievedScalingParameters.getDesiredReplicationCount(),
                     scalingParameters.getDesiredReplicationCount());
        assertEquals(retrievedScalingParameters.getDesiredPartitionCount(),
                     scalingParameters.getDesiredPartitionCount());
    }

}
