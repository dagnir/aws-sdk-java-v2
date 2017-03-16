package software.amazon.awssdk.services.elasticsearch;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.elasticsearch.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticsearch.model.CreateElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.DeleteElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainConfigRequest;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainsRequest;
import software.amazon.awssdk.services.elasticsearch.model.DomainInfo;
import software.amazon.awssdk.services.elasticsearch.model.ElasticsearchDomainConfig;
import software.amazon.awssdk.services.elasticsearch.model.ElasticsearchDomainStatus;
import software.amazon.awssdk.services.elasticsearch.model.ListDomainNamesRequest;
import software.amazon.awssdk.services.elasticsearch.model.ListTagsRequest;
import software.amazon.awssdk.services.elasticsearch.model.Tag;
import software.amazon.awssdk.test.AwsTestBase;


public class ServiceIntegrationTest extends AwsTestBase {

    private static ElasticsearchClient es;

    private static final String DOMAIN_NAME = "java-es-test-" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
        setUpCredentials();
        es = ElasticsearchClient.builder().withCredentials(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @AfterClass
    public static void tearDown() {
        es.deleteElasticsearchDomain(new DeleteElasticsearchDomainRequest()
                .withDomainName(DOMAIN_NAME));
    }

    @Test
    public void testOperations() {

        String domainArn = testCreateDomain();

        testListDomainNames();

        testDescribeDomain();
        testDescribeDomains();
        testDescribeDomainConfig();

        testAddAndListTags(domainArn);

    }

    private String testCreateDomain() {
        ElasticsearchDomainStatus status = es.createElasticsearchDomain(new CreateElasticsearchDomainRequest()
                .withDomainName(DOMAIN_NAME)).getDomainStatus();

        assertEquals(DOMAIN_NAME, status.getDomainName());
        assertValidDomainStatus(status);

        return status.getARN();
    }

    private void testDescribeDomain() {
        ElasticsearchDomainStatus status = es.describeElasticsearchDomain(
                new DescribeElasticsearchDomainRequest()
                        .withDomainName(DOMAIN_NAME)).getDomainStatus();
        assertEquals(DOMAIN_NAME, status.getDomainName());
        assertValidDomainStatus(status);
    }

    private void testDescribeDomains() {
        ElasticsearchDomainStatus status = es
                .describeElasticsearchDomains(
                        new DescribeElasticsearchDomainsRequest()
                                .withDomainNames(DOMAIN_NAME))
                .getDomainStatusList().get(0);
        assertEquals(DOMAIN_NAME, status.getDomainName());
        assertValidDomainStatus(status);
    }

    private void testListDomainNames() {
        List<String> domainNames = toDomainNameList(es.listDomainNames(
                new ListDomainNamesRequest()).getDomainNames());
        assertThat(domainNames, hasItem(DOMAIN_NAME));
    }

    private List<String> toDomainNameList(Collection<DomainInfo> domainInfos) {
        List<String> names = new LinkedList<String>();
        for (DomainInfo info : domainInfos) {
            names.add(info.getDomainName());
        }
        return names;
    }

    private void testDescribeDomainConfig() {
        ElasticsearchDomainConfig config = es
                .describeElasticsearchDomainConfig(
                        new DescribeElasticsearchDomainConfigRequest()
                                .withDomainName(DOMAIN_NAME)).getDomainConfig();
        assertValidDomainConfig(config);
    }

    private void testAddAndListTags(String arn) {
        Tag tag = new Tag().withKey("name").withValue("foo");

        es.addTags(new AddTagsRequest().withARN(arn).withTagList(tag));

        List<Tag> tags = es.listTags(new ListTagsRequest().withARN(arn)).getTagList();
        assertThat(tags, hasItem(tag));
    }

    private void assertValidDomainStatus(ElasticsearchDomainStatus status) {
        assertTrue(status.getCreated());
        assertNotNull(status.getARN());
        assertNotNull(status.getAccessPolicies());
        assertNotNull(status.getAdvancedOptions());
        assertNotNull(status.getDomainId());
        assertNotNull(status.getEBSOptions());
        assertNotNull(status.getElasticsearchClusterConfig());
        assertNotNull(status.getSnapshotOptions());
    }

    private void assertValidDomainConfig(ElasticsearchDomainConfig config) {
        assertNotNull(config.getAccessPolicies());
        assertNotNull(config.getAdvancedOptions());
        assertNotNull(config.getEBSOptions());
        assertNotNull(config.getElasticsearchClusterConfig());
        assertNotNull(config.getSnapshotOptions());
    }

}
