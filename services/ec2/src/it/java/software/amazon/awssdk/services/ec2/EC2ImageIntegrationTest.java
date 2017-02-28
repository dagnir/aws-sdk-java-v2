package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Test;

import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.ec2.model.CopyImageRequest;
import software.amazon.awssdk.services.ec2.model.CopyImageResult;
import software.amazon.awssdk.services.ec2.model.DeregisterImageRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImageAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImageAttributeResult;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.LaunchPermission;
import software.amazon.awssdk.services.ec2.model.ModifyImageAttributeRequest;
import software.amazon.awssdk.services.ec2.model.RegisterImageRequest;
import software.amazon.awssdk.services.ec2.model.RegisterImageResult;
import software.amazon.awssdk.services.ec2.model.ResetImageAttributeRequest;

/**
 * Integration tests for the EC2 Image operations.
 *
 * @author fulghum@amazon.com
 */
public class EC2ImageIntegrationTest extends EC2IntegrationTestBase {

    /**
     * A pre-bundled image in S3 to use to test image registration.
     *
     * Note: This pre-bundled image was bundled by the aws-dr-tools-test@ account
     *       and can only be registered by that account.  This test will only run
     *       correctly for that account and will fail with permission errors for
     *       any other account.
     */
    private static final String TEST_AMI_LOCATION = "aws-sdk-amis/quickstart/image.manifest.xml";

    /**
     * The ID of the new AMI registered during the image registration test.
     * Subsequent tests will use this same AMI to test describing attributes,
     * modifying attributes, reseting attributes, and deregistering.
     */
    private String registeredImageId;


    /**
     * Ensures that any EC2 resources are correctly released after the tests.
     */
    @After
    public void tearDown() {
        if (registeredImageId == null) return;

        DeregisterImageRequest request = new DeregisterImageRequest();
        ec2.deregisterImage(request.withImageId(registeredImageId));
    }

    /**
     * Runs the individual tests in a specific order, so that we hit all image
     * operations, and so that tests that require data from earlier tests run in
     * the correct order.
     */
    @Test
    public void testImageOperations() throws Exception {
        testRegisterImage();

        testDescribeImages();

        testCopyImageAttribute();

        testDescribeImagesByOwner();
        testDescribeImagesById();
        testDescribeImagesWithFilter();

        testDescribeImageAttribute();

        testModifyImageAttribute();
        testResetImageAttribute();

        testDeregisterImage();
    }


    /*
     * Individual Tests
     */

    /**
     * Tests that the RegisterImage operation correctly creates a new AMI for
     * our pre-bundled AMI.
     */
    private void testRegisterImage() {
        RegisterImageRequest request = new RegisterImageRequest();
        request.setImageLocation(TEST_AMI_LOCATION);
        RegisterImageResult result = ec2.registerImage(request);
        assertNotNull(result);

        registeredImageId = result.getImageId();
        assertNotNull(registeredImageId);

        // Tag it
        tagResource(registeredImageId, TAGS);
    }

    /**
     * Tests that the deregisterImage operation sends the request to EC2
     * correctly and removes the AMI we created at the beginning of these tests.
     */
    private void testDeregisterImage() throws Exception {
        assertTrue(doesImageExist(registeredImageId));

        DeregisterImageRequest request = new DeregisterImageRequest();
        ec2.deregisterImage(request.withImageId(registeredImageId));

        int attempt = 0;
        do {
            if (!doesImageExist(registeredImageId)) {
                break;
            } else {
                Thread.sleep(1000);
            }
        } while (attempt++ < 10);

        assertFalse(doesImageExist(registeredImageId));
        registeredImageId = null;
    }

    /**
     * Tests that the library is able to modify the attributes on the image we
     * created at the beginning of these tests.
     */
    private void testModifyImageAttribute() {
        String group = "all";
        assertFalse(doesAmiHaveLaunchPermission(registeredImageId, group));

        ModifyImageAttributeRequest request = new ModifyImageAttributeRequest();
        request.setImageId(registeredImageId);
        // TODO: we should provide constants, or enums for string values like these
        request.setAttribute("launchPermission");
        request.setOperationType("add");
        request.withUserGroups(group);
        ec2.modifyImageAttribute(request);
        try {Thread.sleep(1000 * 20);} catch(Exception e) {}

        assertTrue(doesAmiHaveLaunchPermission(registeredImageId, group));
    }

    /**
     * Tests that we can copy an image.
     */
   private void testCopyImageAttribute() {
       CopyImageRequest copyImageRequest = new CopyImageRequest();
       copyImageRequest.setSourceImageId(registeredImageId);
       copyImageRequest.setSourceRegion("us-east-1");
       CopyImageResult copyImageResult = ec2.copyImage(copyImageRequest);
       assertNotNull(copyImageResult.getImageId());
       // Now in this endpoint, we return a fake image id. When it push to prod, we will try to
       // the valid image back.
   }

    /**
     * Tests that the library is able to reset the image attribute we modified
     * in an earlier test on the image we created.
     */
    private void testResetImageAttribute() {
        assertTrue(doesAmiHaveLaunchPermission(registeredImageId, "all"));

        ResetImageAttributeRequest request = new ResetImageAttributeRequest();
        request.setImageId(registeredImageId);
        request.setAttribute("launchPermission");
        ec2.resetImageAttribute(request);
        try {Thread.sleep(1000 * 20);} catch(Exception e) {}

        assertFalse(doesAmiHaveLaunchPermission(registeredImageId, "all"));
    }

    /**
     * Tests that the no-arg DescribeImages method form correctly returns a list
     * of all the images.
     */
    private void testDescribeImages() {
        /*
         * TODO: We also need to test performance of the marshalling code.
         */


         DescribeImagesResult result = ec2.describeImages();
         List<Image> images = result.getImages();
         assertTrue(images.size() > 1000);
    }

    /**
     * Tests that the OwnerId request parameter is correctly sent to EC2.
     */
    private void testDescribeImagesByOwner() {
        DescribeImagesRequest request = new DescribeImagesRequest();
        ec2.describeImages(request.withOwners("amazon"));
        DescribeImagesResult result = ec2.describeImages(request);

        List<Image> images = result.getImages();
        assertTrue(images.size() > 10);
        for (Image image : images) {
            assertValidImage(image);
        }
    }

    /**
     * Tests that the ImageId request parameter is correctly sent to EC2.
     */
    private void testDescribeImagesById() {
        DescribeImagesRequest request = new DescribeImagesRequest();
        DescribeImagesResult result = ec2.describeImages(request.withImageIds(registeredImageId));
        List<Image> images = result.getImages();
        assertEquals(1, images.size());
        assertValidImage(images.get(0));
        assertEqualUnorderedTagLists(TAGS, images.get(0).getTags());
    }

    /**
     * Tests that the we can use filters to limit Image descriptions.
     */
    private void testDescribeImagesWithFilter() {
        DescribeImagesRequest request = new DescribeImagesRequest();
        request.withFilters(new Filter("image-id", null).withValues(registeredImageId));
        DescribeImagesResult result = ec2.describeImages(request);
        List<Image> images = result.getImages();
        assertEquals(1, images.size());
        assertValidImage(images.get(0));
    }

    /**
     * Tests that the DescribeImageAttribute operation correctly returns image
     * attributes for images.
     */
    private void testDescribeImageAttribute() {
        DescribeImageAttributeRequest request = new DescribeImageAttributeRequest();
        request.setImageId(registeredImageId);
        request.setAttribute("launchPermission");

        DescribeImageAttributeResult result = ec2.describeImageAttribute(request);
        assertEquals(registeredImageId, result.getImageAttribute().getImageId());
    }


    /*
     * Helper Methods
     */

    /**
     * Returns true if the specified Amazon Machine Image ID exists.
     *
     * @param imageId
     *            The AMI ID to check.
     * @return True if the AMI exists, otherwise false.
     */
    private boolean doesImageExist(String imageId) {
        DescribeImagesRequest request = new DescribeImagesRequest();
        DescribeImagesResult result = ec2.describeImages(request.withImageIds(imageId));

        return result.getImages().size() > 0;
    }

    /**
     * Asserts that an Amazon Machine Image object is populated.
     *
     * @param image The image object to test.
     */
    private void assertValidImage(Image image) {
        assertNotNull(image);
        assertNotNull(image.getArchitecture());
        assertNotNull(image.getImageId());
        assertNotNull(image.getImageLocation());
        assertNotNull(image.getOwnerId());
        assertNotNull(image.getImageType());
        // TODO: product codes
    }

    /**
     * Returns true if the specified AMI has a launch permission for the
     * specified group.
     *
     * @param imageId
     *            The ID of an Amazon Machine Image.
     * @param group
     *            The launch permission user group to test for.
     *
     * @return True if the specified AMI has a launch permission for the
     *         specified group, false otherwise.
     */
    private boolean doesAmiHaveLaunchPermission(String imageId, String group) {
        DescribeImageAttributeRequest request = new DescribeImageAttributeRequest();
        request.setImageId(imageId);
        request.setAttribute("launchPermission");
        DescribeImageAttributeResult result = ec2.describeImageAttribute(request);

        List<LaunchPermission> launchPermissions = result.getImageAttribute().getLaunchPermissions();
        for (LaunchPermission launchPermission : launchPermissions) {
            if (launchPermission.getGroup().equals(group)) return true;
        }

        return false;
    }

}
