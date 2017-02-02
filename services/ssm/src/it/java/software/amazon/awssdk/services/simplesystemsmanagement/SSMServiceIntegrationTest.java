package software.amazon.awssdk.services.simplesystemsmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Test;

import software.amazon.awssdk.services.simplesystemsmanagement.model.CreateDocumentRequest;
import software.amazon.awssdk.services.simplesystemsmanagement.model.CreateDocumentResult;
import software.amazon.awssdk.services.simplesystemsmanagement.model.DeleteDocumentRequest;
import software.amazon.awssdk.services.simplesystemsmanagement.model.DescribeDocumentRequest;
import software.amazon.awssdk.services.simplesystemsmanagement.model.DescribeDocumentResult;
import software.amazon.awssdk.services.simplesystemsmanagement.model.DocumentDescription;
import software.amazon.awssdk.services.simplesystemsmanagement.model.GetDocumentRequest;
import software.amazon.awssdk.services.simplesystemsmanagement.model.GetDocumentResult;
import software.amazon.awssdk.services.simplesystemsmanagement.model.ListDocumentsResult;
import software.amazon.awssdk.util.IOUtils;

public class SSMServiceIntegrationTest extends IntegrationTestBase {

    private static final Log LOG = LogFactory.getLog(SSMServiceIntegrationTest.class);
    private static final String DOCUMENT_LOCATION = "documentContent.json";
    private static final String DOCUMENT_NAME = "my-document-" + System.currentTimeMillis();

    @Test
    public void testAll() throws Exception {

        String documentContent = IOUtils.toString(getClass().getResourceAsStream(DOCUMENT_LOCATION));
        testCreateDocument(DOCUMENT_NAME, documentContent);
        testDescribeDocument();
    }

    private void testDescribeDocument() {
        DescribeDocumentResult result = ssm.describeDocument(new DescribeDocumentRequest().withName(DOCUMENT_NAME));
        assertNotNull(result.getDocument());
    }

    @AfterClass
    public static void tearDown() {
        try {
            ssm.deleteDocument(new DeleteDocumentRequest().withName(DOCUMENT_NAME));
        } catch (Exception e) {
            LOG.error("Failed to delete config document.", e);
        }
    }

    private void testCreateDocument(String docName, String docContent) {

        CreateDocumentResult createResult = ssm
                .createDocument(new CreateDocumentRequest().withName(docName).withContent(docContent));

        DocumentDescription description = createResult.getDocumentDescription();

        assertEquals(docName, description.getName());
        assertNotNull(description.getStatus());
        assertNotNull(description.getCreatedDate());

        GetDocumentResult getResult = ssm.getDocument(new GetDocumentRequest().withName(docName));

        assertEquals(DOCUMENT_NAME, getResult.getName());
        assertEquals(docContent, getResult.getContent());

        ListDocumentsResult listResult = ssm.listDocuments();

        assertFalse("ListDocuments should at least returns one element", listResult.getDocumentIdentifiers().isEmpty());

    }

}
