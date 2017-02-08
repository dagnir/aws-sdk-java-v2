package software.amazon.awssdk.services.simpledb.model.transform;

import static org.junit.Assert.assertTrue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import org.junit.Test;
import software.amazon.awssdk.runtime.transform.StaxUnmarshallerContext;
import software.amazon.awssdk.services.simpledb.model.ListDomainsResult;

public class ListDomainsResultUnmarshallerTest {

    /**
     * Test method for ListDomainsResultUnmarshaller
     */
    @Test
    public final void testUnmarshall() throws Exception {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(DomainMetadataResultUnmarshallerTest.class
                .getResourceAsStream("ListDomainsResponse.xml"));
        StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader);
        ListDomainsResult result = (ListDomainsResult) new ListDomainsResultStaxUnmarshaller()
                .unmarshall(unmarshallerContext);

        assertTrue(!result.getDomainNames().isEmpty());
        assertTrue(result.getDomainNames().size() == 2);
        assertTrue(result.getDomainNames().get(0).equals("DomainOne"));
        assertTrue(result.getDomainNames().get(1).equals("DomainTwo"));
    }
}
