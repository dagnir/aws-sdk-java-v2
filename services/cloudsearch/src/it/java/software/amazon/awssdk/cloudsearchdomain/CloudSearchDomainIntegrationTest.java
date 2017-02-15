package software.amazon.awssdk.cloudsearchdomain;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchRequest;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchResult;
import software.amazon.awssdk.services.cloudsearchdomain.model.SuggestRequest;
import software.amazon.awssdk.services.cloudsearchdomain.model.SuggestResult;
import software.amazon.awssdk.services.cloudsearchdomain.model.UploadDocumentsRequest;

public class CloudSearchDomainIntegrationTest extends
                                              CloudSearchDomainIntegrationTestBase {

    @BeforeClass
    public static void setup() throws Exception {
        CloudSearchDomainIntegrationTestBase.setup();
        cloudSearchDomain.setEndpoint(searchServiceEndpoint);
    }

    @Test
    public void testUploadDocumentsWithRandomData()
            throws AmazonServiceException, AmazonClientException,
                   FileNotFoundException {
        try {
            cloudSearchDomain.setEndpoint(docServiceEndpoint);
            cloudSearchDomain.uploadDocuments(new UploadDocumentsRequest()
                                                      .withDocuments(new RepeatableFileInputStream(fileToUpload))
                                                      .withContentType("application/json")
                                                      .withContentLength(fileToUpload.length()));
            fail("Should throw an exception as the data upload is a random and the service wouldn't be able to index it.");
        } catch (AmazonServiceException ase) {
        }
    }

    @Test
    public void testSimpleSearch() {
        SearchResult searchResult = cloudSearchDomain
                .search(new SearchRequest().withQuery("Rush"));
        assertNotNull(searchResult);
        assertNotNull(searchResult.getHits());
        assertTrue(searchResult.getHits().getFound() > 0);
    }

    @Test
    public void testSimpleSearchWithDataNotInDocuments() {
        SearchResult searchResult = cloudSearchDomain
                .search(new SearchRequest().withQuery("abcdefgh"));
        assertNotNull(searchResult);
        assertNotNull(searchResult.getHits());
        assertTrue(searchResult.getHits().getFound() == 0);
    }

    @Test
    public void testSuggestersWithDataIntheDocuments() {
        SuggestResult suggestResult = cloudSearchDomain
                .suggest(new SuggestRequest().withQuery("Thor").withSuggester(
                        suggesterName));
        assertNotNull(suggestResult);
        assertNotNull(suggestResult.getSuggest());
        assertNotNull(suggestResult.getSuggest().getFound() > 0);
    }

    @Test
    public void testSuggestersWithInCorrectDataInQuery() {
        SuggestResult suggestResult = cloudSearchDomain
                .suggest(new SuggestRequest().withQuery("asjdsdf").withSuggester(
                        suggesterName));
        assertNotNull(suggestResult);
        assertNotNull(suggestResult.getSuggest());
        assertNotNull(suggestResult.getSuggest().getFound() == 0);
    }

    @Test
    public void testSuggestersWithInCorrectSuggester() {
        try {
            cloudSearchDomain.suggest(new SuggestRequest().withQuery("Thor")
                                                          .withSuggester("suggesternotpresent"));
            fail("An exception should be thrown as the suggester mentioned in the request is not available");
        } catch (Exception e) {
        }
    }
}
