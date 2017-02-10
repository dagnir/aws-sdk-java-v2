
package software.amazon.awssdk.services.s3;

import static org.junit.Assert.fail;

import java.net.URI;
import org.junit.Assert;
import org.junit.Test;

public class AmazonS3URITest {
    @Test
    public void testRoot() {
        AmazonS3URI uri = new AmazonS3URI("https://s3.amazonaws.com/");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertNull(uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertNull(uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testRegionRoot() {
        AmazonS3URI uri =
                new AmazonS3URI("https://s3.us-west-1.amazonaws.com/");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertNull(uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testRegionRoot2() {
        AmazonS3URI uri =
                new AmazonS3URI("https://s3-us-west-1.amazonaws.com/");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertNull(uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testRegionRoot3() {
        AmazonS3URI uri =
                new AmazonS3URI("https://s3.foobar.baz.qux/");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertNull(uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertEquals("foobar", uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testPathStyleBucket() {
        AmazonS3URI uri = new AmazonS3URI("https://s3.amazonaws.com/bucket");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertNull(uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testPathStyleBucket2() {
        AmazonS3URI uri = new AmazonS3URI("https://s3.amazonaws.com/bucket/");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertNull(uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testHostStyleBucket() {
        AmazonS3URI uri = new AmazonS3URI("https://bucket.s3.amazonaws.com/");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertNull(uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testHostStyleBucket2() {
        AmazonS3URI uri =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testHostStyleBucket3() {
        AmazonS3URI uri =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testPathStyle() {
        AmazonS3URI uri =
                new AmazonS3URI("https://s3.amazonaws.com/bucket/key");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertNull(uri.getRegion());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testHostStyle() {
        AmazonS3URI uri =
                new AmazonS3URI("https://bucket.s3.amazonaws.com/k/e/y");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("k/e/y", uri.getKey());
        Assert.assertNull(uri.getRegion());
    }

    @Test
    public void testHostStyle2() {
        AmazonS3URI uri =
                new AmazonS3URI("https://bu.ck.et.s3.amazonaws.com///k/e/y");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bu.ck.et", uri.getBucket());
        Assert.assertEquals("//k/e/y", uri.getKey());
        Assert.assertNull(uri.getRegion());
    }

    @Test
    public void testPathStyleRegion() {
        AmazonS3URI uri =
                new AmazonS3URI("https://s3.us-west-1.amazonaws.com/bucket/key");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
    }

    @Test
    public void testPathStyleRegion2() {
        AmazonS3URI uri =
                new AmazonS3URI("https://s3-us-west-1.amazonaws.com/bucket/key");

        Assert.assertTrue(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
    }

    @Test
    public void testHostStyleRegion() {
        AmazonS3URI uri =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
    }

    @Test
    public void testHostStyleRegion2() {
        AmazonS3URI uri =
                new AmazonS3URI("https://bucket.s3-us-west-1.amazonaws.com/key");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-1", uri.getRegion());
    }

    @Test
    public void testHostStyleRegion3() {
        AmazonS3URI uri =
                new AmazonS3URI("https://bu.ck.et.s3.foo.bar.baz///k/e/y");

        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("bu.ck.et", uri.getBucket());
        Assert.assertEquals("//k/e/y", uri.getKey());
        Assert.assertEquals("foo", uri.getRegion());
    }

    @Test
    public void testS3Style() {
        AmazonS3URI uri =
                new AmazonS3URI("s3://foo");
        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("foo", uri.getBucket());
        Assert.assertNull(uri.getKey());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testS3Style2() {
        AmazonS3URI uri =
                new AmazonS3URI("s3://foo/bar");
        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("foo", uri.getBucket());
        Assert.assertEquals("bar", uri.getKey());
        Assert.assertNull(uri.getVersionId());
    }

    @Test
    public void testS3Style3() {
        AmazonS3URI uri =
                new AmazonS3URI("s3://foo.123/bar");
        Assert.assertFalse(uri.isPathStyle());
        Assert.assertEquals("foo.123", uri.getBucket());
        Assert.assertEquals("bar", uri.getKey());
        Assert.assertNull(uri.getVersionId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testS3Style4() {
        new AmazonS3URI("s3:///bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testS3Style5() {
        new AmazonS3URI("s4://bar");
    }

    @Test
    public void testPreEncodedKey() {
        AmazonS3URI uriEncoded =
                new AmazonS3URI("https://s3.amazonaws.com/bucket/k%20e%20y");
        Assert.assertTrue(uriEncoded.isPathStyle());
        Assert.assertEquals("bucket", uriEncoded.getBucket());
        Assert.assertEquals("k e y", uriEncoded.getKey());
        Assert.assertNull(uriEncoded.getRegion());

        AmazonS3URI uriNotEncoded =
                new AmazonS3URI("https://s3.amazonaws.com/bucket/k%20e%20y", false);
        Assert.assertTrue(uriNotEncoded.isPathStyle());
        Assert.assertEquals("bucket", uriNotEncoded.getBucket());
        Assert.assertEquals("k e y", uriNotEncoded.getKey());
        Assert.assertNull(uriNotEncoded.getRegion());

    }

    @Test
    public void testPreEncodedBucket() {
        AmazonS3URI uriEncoded = new AmazonS3URI("https://s3.amazonaws.com/bu%2fck%2Fet/key");
        Assert.assertTrue(uriEncoded.isPathStyle());
        Assert.assertEquals("bu/ck/et", uriEncoded.getBucket());
        Assert.assertEquals("key", uriEncoded.getKey());
        Assert.assertNull(uriEncoded.getRegion());

        AmazonS3URI uriNotEncoded = new AmazonS3URI("https://s3.amazonaws.com/bu%2fck%2Fet/key", false);
        Assert.assertTrue(uriNotEncoded.isPathStyle());
        Assert.assertEquals("bu/ck/et", uriNotEncoded.getBucket());
        Assert.assertEquals("key", uriNotEncoded.getKey());
        Assert.assertNull(uriNotEncoded.getRegion());
    }

    @Test
    public void testSpacesKey() {
        AmazonS3URI uriEncoded =
                new AmazonS3URI("s3://foo123/bar/b a z/file");
        Assert.assertFalse(uriEncoded.isPathStyle());
        Assert.assertEquals("foo123", uriEncoded.getBucket());
        Assert.assertEquals("bar/b a z/file", uriEncoded.getKey());

        try {
            new AmazonS3URI("s3://foo123/bar/b a z/file", false);
            fail("An exception should be thrown since un-encoded spaces are present");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testReservedCharactersKey() {
        AmazonS3URI uriEncoded =
                new AmazonS3URI("s3://foo123/!-_.*'()[]@+:&?/file");
        Assert.assertFalse(uriEncoded.isPathStyle());
        Assert.assertEquals("foo123", uriEncoded.getBucket());
        Assert.assertEquals("!-_.*'()[]@+:&?/file", uriEncoded.getKey());

        try {
            new AmazonS3URI("s3://foo123/!-_.*'()[]@+:&?/file", false);
            fail("An exception should be thrown since un-encoded reserved characters are present");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Specifically tests case from tt0075466876
     */
    @Test
    public void testQueryParams() {
        AmazonS3URI uriEncoded =
                new AmazonS3URI("https://bucket.s3.amazonaws.com/key?versionId=123&foo=456");
        Assert.assertEquals(uriEncoded.getURI().getPath(), "/key?versionId=123&foo=456");
        Assert.assertNull(uriEncoded.getURI().getQuery());


        AmazonS3URI uriNotEncoded =
                new AmazonS3URI("https://bucket.s3.amazonaws.com/key?versionId=123&foo=456", false);
        Assert.assertEquals(uriNotEncoded.getURI().getPath(), "/key");
        Assert.assertEquals(uriNotEncoded.getURI().getQuery(), "versionId=123&foo=456");
    }

    @Test
    public void testMultipleQueryParams() {
        AmazonS3URI uri  = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?unrelated=true&versionId=%61%62%63123&unrelated=true"));

        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-2", uri.getRegion());
        Assert.assertEquals("abc123", uri.getVersionId());
    }

    @Test
    public void testMultipleQueryParamsWithSemicolons() {
        AmazonS3URI uri  = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?unrelated=true;versionId=abc%3B%31%32%33;unrelated=true"));

        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-2", uri.getRegion());
        Assert.assertEquals("abc;123", uri.getVersionId());
    }

    @Test
    public void testVersionId() {
        AmazonS3URI uri  = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?versionId=abc123"));

        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-2", uri.getRegion());
        Assert.assertEquals("abc123", uri.getVersionId());
    }

    @Test
    public void testEncodedVersionId() {
        AmazonS3URI uri  = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?versionId=%61%62%63%26123"));

        Assert.assertEquals("bucket", uri.getBucket());
        Assert.assertEquals("key", uri.getKey());
        Assert.assertEquals("us-west-2", uri.getRegion());
        Assert.assertEquals("abc&123", uri.getVersionId());
    }

    @Test
    public void testEqualsWithVersionId() {
        AmazonS3URI one = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?versionId=abc123"));

        AmazonS3URI two = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?versionId=abc123"));

        Assert.assertEquals(one, two);
        Assert.assertEquals(one.hashCode(), two.hashCode());
    }

    @Test
    public void testNotEqualsWithVersionId() {
        AmazonS3URI one = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?versionId=abc123"));

        AmazonS3URI two = new AmazonS3URI(URI.create(
                "https://s3-us-west-2.amazonaws.com/bucket/key"
                        + "?versionId=def456"));

        Assert.assertNotEquals(one, two);
        Assert.assertNotEquals(one.hashCode(), two.hashCode());
    }

    @Test
    public void testIdenticalUrisHashCodeEqual() {
        AmazonS3URI uri1 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key");
        AmazonS3URI uri2 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key");
        Assert.assertEquals(uri1.hashCode(), uri2.hashCode());
    }

    @Test
    public void testDifferentUrisHashCodeNotEqual() {
        AmazonS3URI uri1 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key1");
        AmazonS3URI uri2 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key2");
        Assert.assertNotEquals(uri1.hashCode(), uri2.hashCode());
    }

    @Test
    public void testIdenticalUrisEqual() {
        AmazonS3URI uri1 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key");
        AmazonS3URI uri2 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key");
        Assert.assertEquals(uri1, uri2);
        Assert.assertEquals(uri1.hashCode(), uri2.hashCode());
    }

    @Test
    public void testDifferentUrisNotEqual() {
        AmazonS3URI uri1 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key1");
        AmazonS3URI uri2 =
                new AmazonS3URI("https://bucket.s3.us-west-1.amazonaws.com/key2");
        Assert.assertNotEquals(uri1, uri2);
        Assert.assertNotEquals(uri1.hashCode(), uri2.hashCode());
    }
}
