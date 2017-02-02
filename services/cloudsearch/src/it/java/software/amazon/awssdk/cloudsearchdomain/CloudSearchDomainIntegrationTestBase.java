package software.amazon.awssdk.cloudsearchdomain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.cloudsearchdomain.AmazonCloudSearchDomain;
import software.amazon.awssdk.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import software.amazon.awssdk.services.cloudsearchdomain.model.UploadDocumentsRequest;
import software.amazon.awssdk.services.cloudsearchdomain.model.UploadDocumentsResult;
import software.amazon.awssdk.services.cloudsearchv2.AmazonCloudSearch;
import software.amazon.awssdk.services.cloudsearchv2.AmazonCloudSearchClient;
import software.amazon.awssdk.services.cloudsearchv2.model.CreateDomainRequest;
import software.amazon.awssdk.services.cloudsearchv2.model.DefineIndexFieldRequest;
import software.amazon.awssdk.services.cloudsearchv2.model.DefineIndexFieldResult;
import software.amazon.awssdk.services.cloudsearchv2.model.DefineSuggesterRequest;
import software.amazon.awssdk.services.cloudsearchv2.model.DeleteDomainRequest;
import software.amazon.awssdk.services.cloudsearchv2.model.DescribeDomainsRequest;
import software.amazon.awssdk.services.cloudsearchv2.model.DescribeDomainsResult;
import software.amazon.awssdk.services.cloudsearchv2.model.DescribeIndexFieldsRequest;
import software.amazon.awssdk.services.cloudsearchv2.model.DescribeIndexFieldsResult;
import software.amazon.awssdk.services.cloudsearchv2.model.DocumentSuggesterOptions;
import software.amazon.awssdk.services.cloudsearchv2.model.DomainStatus;
import software.amazon.awssdk.services.cloudsearchv2.model.IndexDocumentsRequest;
import software.amazon.awssdk.services.cloudsearchv2.model.IndexDocumentsResult;
import software.amazon.awssdk.services.cloudsearchv2.model.IndexField;
import software.amazon.awssdk.services.cloudsearchv2.model.IndexFieldStatus;
import software.amazon.awssdk.services.cloudsearchv2.model.IndexFieldType;
import software.amazon.awssdk.services.cloudsearchv2.model.Suggester;
import software.amazon.awssdk.services.cloudsearchv2.model.TextArrayOptions;
import software.amazon.awssdk.services.cloudsearchv2.model.TextOptions;
import software.amazon.awssdk.test.AWSTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * @author subramam
 *
 */
public class CloudSearchDomainIntegrationTestBase extends AWSTestBase {

    /** Client reference to the Amazon cloud search service. */
    protected static AmazonCloudSearch cloudSearch;

    /** Client reference to the Amazon cloud search domain service. */
    protected static AmazonCloudSearchDomain cloudSearchDomain;

    /** Holds reference to the status of the newly created domain. */
    protected static DomainStatus domainStatus = null;

    /** Name of the cloud search created for the test. */
    protected static final String domainName = "sdk-domain-name"
            + System.currentTimeMillis();

    /** Name of the temporary file created for the test to hold junk data. */
    protected static String fileName = "sdk-cs-file-"
            + System.currentTimeMillis();

    /** Size of the temporary file created. */
    protected static long sizeInBytes = 2 * 1024 * 1024;

    /** Reference to the file to upload to cloud search domain. */
    protected static RandomTempFile fileToUpload;

    /**
     * Endpoint for the doc service of the cloud search for uploading documents.
     */
    protected static String docServiceEndpoint;

    /**
     * Endpoint for the search service of the cloud search for performing a
     * search.
     */
    protected static String searchServiceEndpoint;

    /** Sleep Timeout for requests. */
    private static long timeout = 120 * 1000;

    /** Suggester used for searching. */
    protected static String suggesterName = "titlesuggest";

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        cloudSearch = new AmazonCloudSearchClient(credentials);
        cloudSearchDomain = new AmazonCloudSearchDomainClient(credentials);
        setUpDomain();
        fileToUpload = new RandomTempFile(fileName, sizeInBytes);
    }

    @AfterClass
    public static void tearDown() {
        if (fileToUpload != null) {
            if (fileToUpload.exists()) {
                fileToUpload.delete();
            }
        }
        cloudSearch.deleteDomain(new DeleteDomainRequest()
            .withDomainName(domainName));
    }

    /**
     * Loads the file with the given name from the
     * tst/resources/cloudsearchdomain directory. Returns a reference to the
     * file.
     *
     * @param fileName
     *            name of the file
     * @return a reference to the file
     */
    protected static File loadFile(String fileName) {
        File testResourcesDirectory = null;

        testResourcesDirectory = new File("tst/resources", "cloudsearchdomain");

        return new File(testResourcesDirectory, fileName);
    }

    /**
     * Sets up a new domain for this testing. Creates a new domain, and adds the
     * schema of the document. Uploads the documents and creates suggesters.
     */
    private static void setUpDomain() throws InterruptedException,
            AmazonServiceException, AmazonClientException,
            FileNotFoundException {
        createDomain();
        addIndexFields();
        uploadDocuments();
        createSuggesters();
        waitUntilDomainIsActive();
    }

    /**
     * Creates a new cloud search domain
     */
    private static void createDomain() throws InterruptedException {
        cloudSearch.createDomain(new CreateDomainRequest()
                .withDomainName(domainName));
        waitUntilDomainIsActive();

        DomainStatus status = cloudSearch.describeDomains(
                new DescribeDomainsRequest().withDomainNames(domainName))
                .getDomainStatusList().get(0);

        searchServiceEndpoint = status.getSearchService().getEndpoint();
        docServiceEndpoint = status.getDocService().getEndpoint();
    }

    /**
     * Creates a new suggester to be used for suggesting options during
     * searches.
     */
    private static void createSuggesters() throws InterruptedException {
        cloudSearch.defineSuggester(new DefineSuggesterRequest()
                .withDomainName(domainName).withSuggester(
                        new Suggester().withSuggesterName(suggesterName)
                                .withDocumentSuggesterOptions(
                                        new DocumentSuggesterOptions()
                                                .withSourceField("title"))));
        waitUntilIndexFieldsIsActive();
    }

    /**
     * Uploads the documents to the cloud search which is then used for
     * searching and suggesting.
     */
    private static void uploadDocuments() throws AmazonServiceException,
            AmazonClientException, FileNotFoundException {

        File file = loadFile("movies.json");
        cloudSearchDomain.setEndpoint(docServiceEndpoint);
        UploadDocumentsResult uploadResult = cloudSearchDomain
                .uploadDocuments(new UploadDocumentsRequest()
                        .withDocuments(new RepeatableFileInputStream(file))
                        .withContentType("application/json")
                        .withContentLength(file.length()));
        assertTrue(uploadResult.getAdds() > 1L);
        assertEquals(uploadResult.getStatus(), "success");
    }

    /**
     * Defines a new text array field.
     */
    private static DefineIndexFieldResult defineTextArrayIndexField(Field field) {
        return cloudSearch
                .defineIndexField(new DefineIndexFieldRequest()
                        .withDomainName(domainName)
                        .withIndexField(
                                new IndexField()
                                        .withIndexFieldName(field.getName())
                                        .withIndexFieldType(
                                                IndexFieldType.TextArray)
                                        .withTextArrayOptions(
                                                new TextArrayOptions()
                                                        .withReturnEnabled(
                                                                field.isReturnEnabled())
                                                        .withHighlightEnabled(
                                                                field.isHighlightEnabled()))));

    }

    /**
     * Defines a new text field.
     */
    private static DefineIndexFieldResult defineTextIndexField(Field field) {
        return cloudSearch
                .defineIndexField(new DefineIndexFieldRequest()
                        .withDomainName(domainName)
                        .withIndexField(
                                new IndexField()
                                        .withIndexFieldName(field.getName())
                                        .withIndexFieldType(IndexFieldType.Text)
                                        .withTextOptions(
                                                new TextOptions()
                                                        .withReturnEnabled(
                                                                field.isReturnEnabled())
                                                        .withHighlightEnabled(
                                                                field.isHighlightEnabled())
                                                        .withSortEnabled(
                                                                field.isSortEnabled()))));

    }

    /**
     * Model class for an index field.
     */
    static class Field {
        private final String name;
        private final boolean returnEnabled;
        private final boolean searchEnabled;
        private final boolean facetEnabled;
        private final boolean sortEnabled;
        private final boolean highlightEnabled;

        public Field(String name, boolean returnEnabled, boolean searchEnabled,
                boolean facetEnabled, boolean sortEnabled,
                boolean highlightEnabled) {
            this.name = name;
            this.returnEnabled = returnEnabled;
            this.searchEnabled = searchEnabled;
            this.facetEnabled = facetEnabled;
            this.sortEnabled = sortEnabled;
            this.highlightEnabled = highlightEnabled;
        }

        public String getName() {
            return name;
        }

        public boolean isReturnEnabled() {
            return returnEnabled;
        }

        public boolean isSearchEnabled() {
            return searchEnabled;
        }

        public boolean isFacetEnabled() {
            return facetEnabled;
        }

        public boolean isSortEnabled() {
            return sortEnabled;
        }

        public boolean isHighlightEnabled() {
            return highlightEnabled;
        }
    }

    /**
     * Adds the schema of the documents to the cloud search
     */
    private static void addIndexFields() throws InterruptedException {

        Field actor = new Field("actors", true, false, false, false, true);
        Field directors = new Field("directors", true, false, false, false,
                true);
        Field title = new Field("title", true, false, false, true, true);
        Field imageUrl = new Field("image_url", true, false, false, false,
                false);
        Field plot = new Field("plot", true, false, false, false, true);

        defineTextArrayIndexField(actor);
        defineTextArrayIndexField(directors);
        defineTextIndexField(title);
        defineTextIndexField(imageUrl);
        defineTextIndexField(plot);
        waitUntilIndexFieldsIsActive();
    }

    /**
     * Waits until the domain is created and active and also the search and doc service endpoints are available.
     */
    private static void waitUntilDomainIsActive() throws InterruptedException {
        DescribeDomainsResult describeDomainsResult = null;

        while (true) {
            describeDomainsResult = cloudSearch
                    .describeDomains(new DescribeDomainsRequest()
                            .withDomainNames(domainName));
            domainStatus = describeDomainsResult.getDomainStatusList().get(0);
            docServiceEndpoint = domainStatus.getDocService().getEndpoint();
            searchServiceEndpoint = domainStatus.getSearchService()
                    .getEndpoint();
            if ((!domainStatus.getProcessing()) && docServiceEndpoint != null
                    && searchServiceEndpoint != null)
                return;
            Thread.sleep(timeout);
        }
    }

    /**
     * Runs the index documents API to index the documents for newly added/modified fields.
     */
    private static IndexDocumentsResult runIndexDocuments() {
        return cloudSearch.indexDocuments(new IndexDocumentsRequest()
                .withDomainName(domainName));
    }

    /**
     * Waits until the all the index fields are active.
     */
    private static void waitUntilIndexFieldsIsActive()
            throws InterruptedException {

        boolean isFieldsActive = false;
        IndexDocumentsResult indexResult = runIndexDocuments();
        List<String> fieldNames = indexResult.getFieldNames();

        DescribeIndexFieldsResult describeResult = null;
        while (!isFieldsActive) {
            isFieldsActive = true;
            describeResult = cloudSearch
                    .describeIndexFields(new DescribeIndexFieldsRequest()
                            .withDomainName(domainName).withFieldNames(
                                    fieldNames));

            List<IndexFieldStatus> indexFieldsStatus = describeResult
                    .getIndexFields();
            for (IndexFieldStatus fieldStatus : indexFieldsStatus) {
                String state = fieldStatus.getStatus().getState();
                if (!(state.equals("Active"))) {
                    isFieldsActive = false;
                    Thread.sleep(timeout);
                }
            }
        }
    }


    protected static class RepeatableFileInputStream extends InputStream {

        private final File file;
        private FileInputStream fis;

        private long bytesReadPastMarkPoint;
        private long markPoint;

        public RepeatableFileInputStream(File file)
                throws FileNotFoundException {

            this.file = file;
            this.fis = new FileInputStream(file);
        }

        @Override
        public boolean markSupported() { return true; }

        @Override
        public void mark(int readLimit) {
            markPoint += bytesReadPastMarkPoint;
            bytesReadPastMarkPoint = 0;
        }

        @Override
        public void reset() throws IOException {
            if (fis != null) {
                fis.close();
            }
            fis = new FileInputStream(file);

            long skipped = 0;
            long toSkip = markPoint;
            while (toSkip > 0) {
                skipped = fis.skip(toSkip);
                toSkip -= skipped;
            }

            bytesReadPastMarkPoint = 0;
        }

        @Override
        public int available() throws IOException {
            return fis.available();
        }

        @Override
        public int read() throws IOException {
            int result = fis.read();
            if (result != -1) {
                bytesReadPastMarkPoint += 1;
            }
            return result;
        }

        @Override
        public int read(byte[] buf, int pos, int len) throws IOException {
            int count = fis.read(buf, pos, len);
            if (count > 0) {
                bytesReadPastMarkPoint += count;
            }
            return count;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = fis.skip(n);
            bytesReadPastMarkPoint += skipped;
            return skipped;
        }

        @Override
        public void close() throws IOException {
            fis.close();
        }
    }
}
