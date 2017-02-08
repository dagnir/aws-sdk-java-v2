/*
 * Copyright 2015-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.s3.transfer;

import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Encryption;
import software.amazon.awssdk.services.s3.internal.ServiceUtils;
import software.amazon.awssdk.services.s3.internal.SkipMd5CheckStrategy;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.transfer.internal.DownloadImpl;

final class DownloadTaskImpl implements
        ServiceUtils.RetryableS3DownloadTask
{
    private final AmazonS3 s3;
    private final DownloadImpl download;
    private final GetObjectRequest getObjectRequest;
    private final SkipMd5CheckStrategy skipMd5CheckStrategy = SkipMd5CheckStrategy.INSTANCE;

    DownloadTaskImpl(AmazonS3 s3, DownloadImpl download,
            GetObjectRequest getObjectRequest) {
        this.s3 = s3;
        this.download = download;
        this.getObjectRequest = getObjectRequest;
    }

    @Override
    public S3Object getS3ObjectStream() {
        S3Object s3Object = s3.getObject(getObjectRequest);
        download.setS3Object(s3Object);
        return s3Object;
    }

    @Override
    public boolean needIntegrityCheck() {
        // Don't perform the integrity check if the checksum won't matchup.
        return !(s3 instanceof AmazonS3Encryption) && !skipMd5CheckStrategy.skipClientSideValidationPerRequest(getObjectRequest);
    }
}
