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

package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.simpledb.model.Attribute;
import software.amazon.awssdk.services.simpledb.model.CreateDomainRequest;
import software.amazon.awssdk.services.simpledb.model.DeleteDomainRequest;
import software.amazon.awssdk.services.simpledb.model.Item;
import software.amazon.awssdk.services.simpledb.model.PutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.ReplaceableAttribute;
import software.amazon.awssdk.services.simpledb.model.SelectRequest;
import software.amazon.awssdk.services.simpledb.model.SelectResult;

/**
 * Tests that we can handle SimpleDBs base64 encoding attributes when users upload data that isn't
 * XML-compatible.
 *
 * @author Jason Fulghum <fulghum@amazon.com>
 */
public class InvalidXmlCharactersIntegrationTest extends IntegrationTestBase {

    private static final String NON_XML_COMPATIBLE_STRING = "\u0001" + "foobar";
    private static String domainName = "invalid-characters-integ-test-" + new Date().getTime();

    /** Releases all resources used by this test */
    @After
    public void tearDown() throws Exception {
        sdb.deleteDomain(new DeleteDomainRequest().withDomainName(domainName));
    }

    /**
     * Tests that when non-XML compatible strings are used in SimpleDB data, that we're correctly
     * able to message to the user when those values have been base64 encoded when they are sent
     * back in service responses.
     */
    @Test
    public void testInvalidXmlCharacters() throws Exception {
        createTestData();

        SelectResult selectResult = sdb.select(new SelectRequest().withSelectExpression("SELECT * FROM `" + domainName
                                                                                        + "`"));

        List<Item> items = selectResult.getItems();
        assertEquals(1, items.size());

        Item item = (Item) items.get(0);
        assertEquals("base64", item.getAlternateNameEncoding());

        assertEquals(1, item.getAttributes().size());
        Attribute attribute = (Attribute) item.getAttributes().get(0);
        assertEquals("base64", attribute.getAlternateNameEncoding());
        assertEquals("base64", attribute.getAlternateValueEncoding());
    }

    /*
     * Private Helper Methods
     */

    /**
     * Creates all the test data required by this test, including the test domain, an item with a
     * name containing a non-XML compatible character, and an attribute with a name and value
     * containing non-XML compatible characters.
     */
    private void createTestData() throws Exception {
        sdb.createDomain(new CreateDomainRequest().withDomainName(domainName));

        ReplaceableAttribute attribute = new ReplaceableAttribute().withName(NON_XML_COMPATIBLE_STRING).withValue(
                NON_XML_COMPATIBLE_STRING);
        sdb.putAttributes(new PutAttributesRequest().withDomainName(domainName).withItemName(NON_XML_COMPATIBLE_STRING)
                                                    .withAttributes(new ReplaceableAttribute[] {attribute}));

        // Sleep 5s to let data propagate
        Thread.sleep(5 * 1000);
    }
}
