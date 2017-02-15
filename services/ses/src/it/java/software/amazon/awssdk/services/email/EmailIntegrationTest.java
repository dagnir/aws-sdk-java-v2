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

package software.amazon.awssdk.services.email;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.simpleemail.model.Body;
import software.amazon.awssdk.services.simpleemail.model.Content;
import software.amazon.awssdk.services.simpleemail.model.DeleteIdentityRequest;
import software.amazon.awssdk.services.simpleemail.model.Destination;
import software.amazon.awssdk.services.simpleemail.model.GetIdentityDkimAttributesRequest;
import software.amazon.awssdk.services.simpleemail.model.GetIdentityDkimAttributesResult;
import software.amazon.awssdk.services.simpleemail.model.GetIdentityVerificationAttributesRequest;
import software.amazon.awssdk.services.simpleemail.model.GetIdentityVerificationAttributesResult;
import software.amazon.awssdk.services.simpleemail.model.GetSendQuotaResult;
import software.amazon.awssdk.services.simpleemail.model.IdentityDkimAttributes;
import software.amazon.awssdk.services.simpleemail.model.IdentityType;
import software.amazon.awssdk.services.simpleemail.model.IdentityVerificationAttributes;
import software.amazon.awssdk.services.simpleemail.model.ListIdentitiesRequest;
import software.amazon.awssdk.services.simpleemail.model.Message;
import software.amazon.awssdk.services.simpleemail.model.MessageRejectedException;
import software.amazon.awssdk.services.simpleemail.model.SendEmailRequest;
import software.amazon.awssdk.services.simpleemail.model.SetIdentityDkimEnabledRequest;
import software.amazon.awssdk.services.simpleemail.model.VerificationStatus;
import software.amazon.awssdk.services.simpleemail.model.VerifyDomainDkimRequest;
import software.amazon.awssdk.services.simpleemail.model.VerifyDomainDkimResult;
import software.amazon.awssdk.services.simpleemail.model.VerifyDomainIdentityRequest;
import software.amazon.awssdk.services.simpleemail.model.VerifyEmailIdentityRequest;

public class EmailIntegrationTest extends IntegrationTestBase {

    private static final String DOMAIN = "invalid-test-domain";
    private static final String EMAIL = "nobody@amazon.com";
    private static String DOMAIN_VERIFICATION_TOKEN;

    @BeforeClass
    public static void setup() {
        email.verifyEmailIdentity(new VerifyEmailIdentityRequest().withEmailAddress(EMAIL));
        DOMAIN_VERIFICATION_TOKEN = email.verifyDomainIdentity(new VerifyDomainIdentityRequest().withDomain(DOMAIN))
                                         .getVerificationToken();

    }

    @AfterClass
    public static void tearDown() {
        email.deleteIdentity(new DeleteIdentityRequest().withIdentity(EMAIL));
        email.deleteIdentity(new DeleteIdentityRequest().withIdentity(DOMAIN));
    }

    @Test
    public void getSendQuota_ReturnsNonZeroQuotas() {
        GetSendQuotaResult result = email.getSendQuota();
        assertThat(result.getMax24HourSend(), greaterThan(0.0));
        assertThat(result.getMaxSendRate(), greaterThan(0.0));
    }

    @Test
    public void listIdentities_WithNonVerifiedIdentity_ReturnsIdentityInList() {
        // Don't need to actually verify for it to show up in listIdentities
        List<String> identities = email.listIdentities().getIdentities();
        assertThat(identities, hasItem(EMAIL));
        assertThat(identities, hasItem(DOMAIN));
    }

    @Test
    public void listIdentities_FilteredForDomainIdentities_OnlyHasDomainIdentityInList() {
        List<String> identities = email.listIdentities(
                new ListIdentitiesRequest().withIdentityType(IdentityType.Domain)).getIdentities();
        assertThat(identities, not(hasItem(EMAIL)));
        assertThat(identities, hasItem(DOMAIN));
    }

    @Test
    public void listIdentities_FilteredForEmailIdentities_OnlyHasEmailIdentityInList() {
        List<String> identities = email.listIdentities(
                new ListIdentitiesRequest().withIdentityType(IdentityType.EmailAddress)).getIdentities();
        assertThat(identities, hasItem(EMAIL));
        assertThat(identities, not(hasItem(DOMAIN)));
    }

    @Test
    public void listIdentitites_MaxResultsSetToOne_HasNonNullNextToken() {
        assertNotNull(email.listIdentities(new ListIdentitiesRequest().withMaxItems(1)).getNextToken());
    }

    @Test(expected = AmazonServiceException.class)
    public void listIdentities_WithInvalidNextToken_ThrowsException() {
        email.listIdentities(new ListIdentitiesRequest().withNextToken("invalid-next-token"));
    }

    @Test(expected = MessageRejectedException.class)
    public void sendEmail_ToUnverifiedIdentity_ThrowsException() {
        email.sendEmail(new SendEmailRequest().withDestination(new Destination().withToAddresses(EMAIL))
                                              .withMessage(newMessage("test")).withSource(EMAIL));
    }

    @Test
    public void getIdentityVerificationAttributes_ForNonVerifiedEmail_ReturnsPendingVerificatonStatus() {
        GetIdentityVerificationAttributesResult result = email
                .getIdentityVerificationAttributes(new GetIdentityVerificationAttributesRequest().withIdentities(EMAIL));
        IdentityVerificationAttributes identityVerificationAttributes = result.getVerificationAttributes().get(EMAIL);
        assertEquals(VerificationStatus.Pending.toString(), identityVerificationAttributes.getVerificationStatus());
        // Verificaton token not applicable for email identities
        assertNull(identityVerificationAttributes.getVerificationToken());
    }

    @Test
    public void getIdentityVerificationAttributes_ForNonVerifiedDomain_ReturnsPendingVerificatonStatus() {
        GetIdentityVerificationAttributesResult result = email
                .getIdentityVerificationAttributes(new GetIdentityVerificationAttributesRequest()
                                                           .withIdentities(DOMAIN));
        IdentityVerificationAttributes identityVerificationAttributes = result.getVerificationAttributes().get(DOMAIN);
        assertEquals(VerificationStatus.Pending.toString(), identityVerificationAttributes.getVerificationStatus());
        assertEquals(DOMAIN_VERIFICATION_TOKEN, identityVerificationAttributes.getVerificationToken());
    }

    @Test
    public void verifyDomainDkim_ChangesDkimVerificationStatusToPending() throws InterruptedException {
        String testDomain = "java-integ-test-dkim-" + System.currentTimeMillis() + ".com";
        try {
            email.verifyDomainIdentity(new VerifyDomainIdentityRequest().withDomain(testDomain));
            GetIdentityDkimAttributesResult result = email
                    .getIdentityDkimAttributes(new GetIdentityDkimAttributesRequest().withIdentities(testDomain));
            assertTrue(result.getDkimAttributes().size() == 1);

            // should be no tokens and no verification
            IdentityDkimAttributes attributes = result.getDkimAttributes().get(testDomain);
            assertFalse(attributes.getDkimEnabled());
            assertEquals(VerificationStatus.NotStarted.toString(), attributes.getDkimVerificationStatus());
            assertThat(attributes.getDkimTokens(), hasSize(0));

            VerifyDomainDkimResult dkim = email.verifyDomainDkim(new VerifyDomainDkimRequest().withDomain(testDomain));
            Thread.sleep(5 * 1000);

            result = email.getIdentityDkimAttributes(new GetIdentityDkimAttributesRequest().withIdentities(testDomain));
            assertTrue(result.getDkimAttributes().size() == 1);

            attributes = result.getDkimAttributes().get(testDomain);
            assertTrue(attributes.getDkimEnabled());
            assertTrue(attributes.getDkimVerificationStatus().equals(VerificationStatus.Pending.toString()));
            assertTrue(attributes.getDkimTokens().size() == dkim.getDkimTokens().size());

            try {
                email.setIdentityDkimEnabled(new SetIdentityDkimEnabledRequest().withIdentity(testDomain));
                fail("Exception should have occurred during enable");
            } catch (AmazonServiceException exception) {
                // exception expected
            }
        } finally {
            // Delete domain from verified list.
            email.deleteIdentity(new DeleteIdentityRequest().withIdentity(testDomain));
        }
    }

    private Message newMessage(String subject) {
        Content content = new Content().withData(subject);
        Message message = new Message().withSubject(content).withBody(new Body().withText(content));

        return message;
    }

}
