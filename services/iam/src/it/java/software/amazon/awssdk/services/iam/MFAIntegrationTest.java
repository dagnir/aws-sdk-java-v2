package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.identitymanagement.model.CreateVirtualMFADeviceRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteVirtualMFADeviceRequest;
import software.amazon.awssdk.services.identitymanagement.model.ListMFADevicesRequest;
import software.amazon.awssdk.services.identitymanagement.model.MFADevice;
import software.amazon.awssdk.services.identitymanagement.model.VirtualMFADevice;

/** Integration tests for the MFA device related APIs in IAM. */
public class MFAIntegrationTest extends IntegrationTestBase {

    // TODO: Remove after IAM launches the MFA device feature publicly (~ Oct 2011)
    static {
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
    }

    private final String deviceName = "java-sdk-mfa-" + System.currentTimeMillis();
    private String serialNumber;


    /** Delete any created resources. */
    @After
    public void tearDown() throws Exception {
        try {
            iam.deleteVirtualMFADevice(new DeleteVirtualMFADeviceRequest().withSerialNumber(serialNumber));
        } catch (Exception e) {
        }
    }

    @Test
    public void testMFADeviceOperations() throws Exception {
        // Create Virtual MFA Device
        VirtualMFADevice mfaDevice = iam.createVirtualMFADevice(new CreateVirtualMFADeviceRequest()
                                                                        .withVirtualMFADeviceName(deviceName)).getVirtualMFADevice();
        serialNumber = mfaDevice.getSerialNumber();
        assertNotNull(serialNumber);
        assertNotNull(mfaDevice.getBase32StringSeed());
        assertNotNull(mfaDevice.getQRCodePNG());
        assertNotNull(mfaDevice.getSerialNumber());

        Thread.sleep(1000 * 10);

        // List MFA Devices
        List<MFADevice> mfaDevices = iam.listMFADevices(new ListMFADevicesRequest().withMaxItems(100)).getMFADevices();
        assertTrue(mfaDevices.size() > 0);
        assertNotNull(mfaDevices.get(0).getSerialNumber());

        // Enable and Resync require codes from a physical MFA device
        //iam.enableMFADevice(new EnableMFADeviceRequest(userName, serialNumber, "", ""));
        //iam.resyncMFADevice(new ResyncMFADeviceRequest(userName, serialNumber, "", ""));

        // Deactivate requires that we previously activated an MFA device
        //iam.deactivateMFADevice(new DeactivateMFADeviceRequest(userName, serialNumber);

        // Delete the virtual MFA device
        iam.deleteVirtualMFADevice(new DeleteVirtualMFADeviceRequest().withSerialNumber(mfaDevice.getSerialNumber()));
    }

}
