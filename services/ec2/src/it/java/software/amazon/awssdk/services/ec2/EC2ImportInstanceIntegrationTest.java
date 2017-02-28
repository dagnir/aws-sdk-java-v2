package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import software.amazon.awssdk.services.ec2.model.CancelConversionTaskRequest;
import software.amazon.awssdk.services.ec2.model.ConversionTask;
import software.amazon.awssdk.services.ec2.model.DiskImage;
import software.amazon.awssdk.services.ec2.model.DiskImageDescription;
import software.amazon.awssdk.services.ec2.model.DiskImageDetail;
import software.amazon.awssdk.services.ec2.model.ImportInstanceLaunchSpecification;
import software.amazon.awssdk.services.ec2.model.ImportInstanceRequest;
import software.amazon.awssdk.services.ec2.model.ImportInstanceResult;
import software.amazon.awssdk.services.ec2.model.ImportInstanceTaskDetails;
import software.amazon.awssdk.services.ec2.model.ImportInstanceVolumeDetailItem;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.VolumeDetail;
import software.amazon.awssdk.services.s3.internal.Constants;

public class EC2ImportInstanceIntegrationTest extends EC2IntegrationTestBase {

    private static final String IMPORT_INSTANCE_MANIFEST_URL = "https://s3.amazonaws.com/MyImportBucket/a3a5e1b6-590d-43cc-97c1-15c7325d3f41/Win_2008_Server_Data_Center_SP2_32-bit.​vmdkmanifest.xml?AWSAccessKeyId=AKIAIR2I45FHYEXAMPLE&Expires=1294855591&Signature=5snej01TlTtL0uR7KExtEXAMPLE%3D";
    /**
     * Tests that the ImportInstance operation could work properly.
     * Especially we want to check that the request parameter LaunchSpecification.GroupName
     * is accepted by EC2.
     */
    @Test
    public void testImportInstance() {
        String description = "ImportInstance Test";
        String imageFormat = "VMDK";
        Long imageSize = Constants.GB;
        Long volumeSize = 10L;

        ImportInstanceRequest request = new ImportInstanceRequest()
            .withDescription(description)
            .withPlatform("Windows")
            .withDiskImages(new DiskImage()
                .withImage(new DiskImageDetail().withImportManifestUrl(IMPORT_INSTANCE_MANIFEST_URL)
                                                .withFormat(imageFormat)
                                                 .withBytes(imageSize))
                .withVolume(new VolumeDetail().withSize(volumeSize)));
        ImportInstanceLaunchSpecification importInstanceLaunchSpecificationLaunchSpecification = new ImportInstanceLaunchSpecification();
        importInstanceLaunchSpecificationLaunchSpecification
            .withArchitecture("x86_64")
            .withGroupNames("default")
            .withInstanceType(InstanceType.T1Micro);
        request.setLaunchSpecification(importInstanceLaunchSpecificationLaunchSpecification);
        ImportInstanceResult importInstanceResult = ec2.importInstance(request);

        ConversionTask conversionTask = importInstanceResult.getConversionTask();
        assertNotNull(conversionTask);
        assertNotNull(conversionTask.getConversionTaskId());
        assertNotNull(conversionTask.getExpirationTime());
        assertEquals("active", conversionTask.getState());
        assertNotNull(conversionTask.getStatusMessage());

        ImportInstanceTaskDetails importInstanceTaskDetails = conversionTask.getImportInstance();
        assertNotNull(importInstanceTaskDetails);
        assertEquals(description, importInstanceTaskDetails.getDescription());
        assertNotNull(importInstanceTaskDetails.getInstanceId());
        assertNotNull(importInstanceTaskDetails.getVolumes());
        assertEquals(1, importInstanceTaskDetails.getVolumes().size());
        ImportInstanceVolumeDetailItem importInstanceVolumeDetailItem = importInstanceTaskDetails.getVolumes().get(0);
        assertNotNull(importInstanceVolumeDetailItem);
        assertNotNull(importInstanceVolumeDetailItem.getAvailabilityZone());
        assertNotNull(importInstanceVolumeDetailItem.getBytesConverted());
        assertEquals("active", importInstanceVolumeDetailItem.getStatus());
        assertNotNull(importInstanceVolumeDetailItem.getStatusMessage());
        assertNotNull(importInstanceVolumeDetailItem.getVolume());
        assertEquals(volumeSize, importInstanceVolumeDetailItem.getVolume().getSize());

        DiskImageDescription diskImageDescription = importInstanceVolumeDetailItem.getImage();
        assertNotNull(diskImageDescription);
        assertEquals(imageFormat, diskImageDescription.getFormat());
        assertEquals(IMPORT_INSTANCE_MANIFEST_URL, diskImageDescription.getImportManifestUrl());
        assertEquals(imageSize, diskImageDescription.getSize());

        // Cancel the conversion task
        ec2.cancelConversionTask(new CancelConversionTaskRequest().withConversionTaskId(conversionTask.getConversionTaskId()));

        // Terminate the imported instance
        ec2.terminateInstances(new TerminateInstancesRequest()
                                    .withInstanceIds(importInstanceTaskDetails.getInstanceId()));

    }
}
