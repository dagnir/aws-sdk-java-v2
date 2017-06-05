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

package software.amazon.awssdk.services.iam;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.iam.model.CreateVirtualMFADeviceRequest;
import software.amazon.awssdk.services.iam.model.DeleteVirtualMFADeviceRequest;
import software.amazon.awssdk.services.iam.model.ListMFADevicesRequest;
import software.amazon.awssdk.services.iam.model.MFADevice;
import software.amazon.awssdk.services.iam.model.VirtualMFADevice;

/** Integration tests for the MFA device related APIs in IAM. */
public class MFAIntegrationTest extends IntegrationTestBase {

    // TODO: Remove after IAM launches the MFA device feature publicly (~ Oct 2011)
    static {
        System.getProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
    }

    private final String deviceName = "java-sdk-mfa-" + System.currentTimeMillis();
    private String serialNumber;


    /** Delete any created resources. */
    @After
    public void tearDown() throws Exception {
        try {
            iam.deleteVirtualMFADevice(DeleteVirtualMFADeviceRequest.builder().serialNumber(serialNumber).build());
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    @Test
    public void testMFADeviceOperations() throws Exception {
        // Create Virtual MFA Device
        VirtualMFADevice mfaDevice = iam.createVirtualMFADevice(CreateVirtualMFADeviceRequest.builder()
                                                                                             .virtualMFADeviceName(deviceName)
                                                                                             .build()).virtualMFADevice();
        serialNumber = mfaDevice.serialNumber();
        assertNotNull(serialNumber);
        assertNotNull(mfaDevice.base32StringSeed());
        assertNotNull(mfaDevice.qrCodePNG());
        assertNotNull(mfaDevice.serialNumber());

        Thread.sleep(1000 * 10);

        // List MFA Devices
        List<MFADevice> mfaDevices = iam.listMFADevices(ListMFADevicesRequest.builder().maxItems(100).build()).mfaDevices();
        assertTrue(mfaDevices.size() > 0);
        assertNotNull(mfaDevices.get(0).serialNumber());

        // Enable and Resync require codes from a physical MFA device
        //iam.enableMFADevice(EnableMFADeviceRequest.builder(userName, serialNumber, "", ""));
        //iam.resyncMFADevice(ResyncMFADeviceRequest.builder(userName, serialNumber, "", ""));

        // Deactivate requires that we previously activated an MFA device
        //iam.deactivateMFADevice(DeactivateMFADeviceRequest.builder(userName, serialNumber);

        // Delete the virtual MFA device
        iam.deleteVirtualMFADevice(DeleteVirtualMFADeviceRequest.builder().serialNumber(mfaDevice.serialNumber()).build());
    }

}
