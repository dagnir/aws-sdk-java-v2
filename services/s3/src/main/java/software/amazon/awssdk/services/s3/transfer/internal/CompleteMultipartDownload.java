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

package software.amazon.awssdk.services.s3.transfer.internal;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.services.s3.internal.ServiceUtils;
import software.amazon.awssdk.services.s3.transfer.Transfer;

/**
 * Helper class to merge all the individual part files into a destinationFile.
 */
@SdkInternalApi
public class CompleteMultipartDownload implements Callable<File> {
    private final List<Future<File>> partFiles;
    private final File destinationFile;
    private final DownloadImpl download;
    private Integer currentPartNumber;

    public CompleteMultipartDownload(List<Future<File>> files, File destinationFile, DownloadImpl download,
                                     Integer currentPartNumber) {
        this.partFiles = files;
        this.destinationFile = destinationFile;
        this.download = download;
        this.currentPartNumber = currentPartNumber;
    }

    @Override
    public File call() throws Exception {
        for (Future<File> file : partFiles) {
            ServiceUtils.appendFile(file.get(), destinationFile);
            download.updatePersistableTransfer(currentPartNumber++);
        }

        download.setState(Transfer.TransferState.Completed);
        return destinationFile;
    }
}
