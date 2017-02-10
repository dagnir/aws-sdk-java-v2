package software.amazon.awssdk.services.s3.iterable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;
import software.amazon.awssdk.services.s3.model.S3VersionSummary;
import software.amazon.awssdk.services.s3.model.VersionListing;

public class S3VersionsForKeyTest extends S3VersionsTestCommon {

    @Before
    public void setUp() throws Exception {
        s3Versions = S3Versions.forKey(s3, "my-bucket", "the-key");
        setSummaryKey("the-key");
    }

    @Test
    public void testSetsBucket() throws Exception {
        assertEquals("my-bucket", s3Versions.getBucketName());
    }

    @Test
    public void testUsesNullPrefix() throws Exception {
        assertNull(s3Versions.getPrefix());
    }

    @Test
    public void testSetsKey() throws Exception {
        assertEquals("the-key", s3Versions.getKey());
    }

    @Test
    public void testStopsAtNonMatchingKey() throws Exception {
        VersionListing listing = mock(VersionListing.class);
        S3VersionSummary matchingSummary = mock(S3VersionSummary.class);
        S3VersionSummary nonMatchingSummary = mock(S3VersionSummary.class);

        when(listing.getVersionSummaries()).thenReturn(
                Arrays.asList(matchingSummary, nonMatchingSummary, matchingSummary));
        when(matchingSummary.getKey()).thenReturn("the-key");
        when(nonMatchingSummary.getKey()).thenReturn("the-other-key");
        when(s3.listVersions(any(ListVersionsRequest.class))).thenReturn(listing);

        Iterator<S3VersionSummary> iter = s3Versions.iterator();
        assertTrue(iter.hasNext());
        assertSame(matchingSummary, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDoesNotRequestNextPageIfNonMatchingKeyWasFound() throws Exception {
        VersionListing listing = mock(VersionListing.class);
        S3VersionSummary matchingSummary = mock(S3VersionSummary.class);
        S3VersionSummary nonMatchingSummary = mock(S3VersionSummary.class);

        when(listing.getVersionSummaries()).thenReturn(
                Arrays.asList(matchingSummary, nonMatchingSummary));
        when(listing.isTruncated()).thenReturn(true);
        when(matchingSummary.getKey()).thenReturn("the-key");
        when(nonMatchingSummary.getKey()).thenReturn("the-other-key");
        when(s3.listVersions(any(ListVersionsRequest.class))).thenReturn(listing);

        Iterator<S3VersionSummary> iter = s3Versions.iterator();
        assertTrue(iter.hasNext());
        assertSame(matchingSummary, iter.next());
        assertFalse(iter.hasNext());

        verify(s3, never()).listNextBatchOfVersions(listing);
    }

    @Test
    public void testSetsKeyAsPrefixOnRequest() throws Exception {
        s3Versions.iterator().hasNext();

        ArgumentCaptor<ListVersionsRequest> listCaptor = ArgumentCaptor.forClass(ListVersionsRequest.class);
        verify(s3).listVersions(listCaptor.capture());
        assertEquals("the-key", listCaptor.getValue().getPrefix());
    }

}
