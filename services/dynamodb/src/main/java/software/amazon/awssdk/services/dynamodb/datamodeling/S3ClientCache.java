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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.Region;
import software.amazon.awssdk.services.s3.transfer.TransferManager;

/**
 * A smart Map for {@link AmazonS3} objects. {@link S3ClientCache} keeps the
 * clients organized by region, and if provided {@link AwsCredentials} will
 * create clients on the fly. Otherwise it just return clients given to it with
 * {@link #useClient(AmazonS3)}.
 */
public class S3ClientCache {
    private final ConcurrentMap<String, AmazonS3> clientsByRegion = new ConcurrentHashMap<String, AmazonS3>();
    private final Map<String, TransferManager> transferManagersByRegion = new ConcurrentHashMap<String, TransferManager>();

    private final AwsCredentialsProvider awscredentialsProvider;

    @Deprecated
    S3ClientCache(AwsCredentials credentials) {
        this(new AwsStaticCredentialsProvider(credentials));
    }

    /**
     * Create a client cache using the given AWSCredentialsProvider. If
     * {@link #getClient(Regions)} or {@link #getTransferManager(Regions)} is
     * called and a client has not been provided for the region, the cache will
     * instantiate one from the provided {@link AwsCredentialsProvider}.
     *
     * @param awsCredentialsProvider
     *            The credentials provider to use when creating new
     *            {@link AmazonS3}.
     */
    S3ClientCache(AwsCredentialsProvider awsCredentialsProvider) {
        this.awscredentialsProvider = awsCredentialsProvider;
    }

    /**
     * Force the client cache to provide a certain client for the region which
     * that client is configured. This can be useful to provide clients with
     * different {@link software.amazon.awssdk.services.s3.S3ClientOptions} or use a
     * {@link software.amazon.awssdk.services.s3.AmazonS3EncryptionClient} in place of a
     * regular client.
     *
     * Using a new client will also forcibly shut down any
     * {@link TransferManager} that has been instantiated with that client, with
     * the {@link TransferManager#shutdownNow()} method.
     *
     * @param client
     *            An {@link AmazonS3} to use in the cache. Its region will
     *            be detected automatically.
     */
    public void useClient(AmazonS3 client) {
        String region = client.getRegionName();

        synchronized (transferManagersByRegion) {
            TransferManager tm = transferManagersByRegion.remove(region);
            if (tm != null) {
                tm.shutdownNow();
            }
            clientsByRegion.put(region, client);
        }
    }

    /**
     * Returns a client for the requested region, or throws an exception when
     * unable.
     *
     * @param region
     *            The region the returned {@link AmazonS3} will be
     *            configured to use.
     * @return A client for the given region from the cache, either instantiated
     *         automatically from the provided {@link AwsCredentials} or
     *         provided with {@link #useClient(AmazonS3)}.
     * @throws IllegalArgumentException
     *             When a region is requested that has not been provided to the
     *             cache with {@link #useClient(AmazonS3)}, and the cache
     *             has no {@link AwsCredentials} with which a client may be
     *             instantiated.
     */
    public AmazonS3 getClient(Region region) {
        if (region == null) {
            throw new IllegalArgumentException("S3 region must be specified");
        }
        return getClient(region.toAwsRegion().getName());
    }

    /**
     * Returns a client for the requested region, or throws an exception when
     * unable.
     *
     * @param region
     *            The region the returned {@link AmazonS3} will be
     *            configured to use.
     * @return A client for the given region from the cache, either instantiated
     *         automatically from the provided {@link AwsCredentials} or
     *         provided with {@link #useClient(AmazonS3)}.
     * @throws IllegalArgumentException
     *             When a region is requested that has not been provided to the
     *             cache with {@link #useClient(AmazonS3)}, and the cache
     *             has no {@link AwsCredentials} with which a client may be
     *             instantiated.
     */
    public AmazonS3 getClient(String region) {
        if (region == null) {
            throw new IllegalArgumentException("S3 region must be specified");
        }
        AmazonS3 client = clientsByRegion.get(region);
        return client != null ? client : cacheClient(region);
    }

    /**
     * Returns a new client with region configured to
     * region.
     * Also updates the clientsByRegion map by associating the
     * new client with region.
     *
     * @param region
     *            The region the returned {@link AmazonS3} will be
     *            configured to use.
     * @return A new {@link AmazonS3} client with region set to region.
     */
    private AmazonS3 cacheClient(String region) {
        if (awscredentialsProvider == null) {
            throw new IllegalArgumentException("No credentials provider found to connect to S3");
        }
        AmazonS3 client = new AmazonS3Client(awscredentialsProvider);
        client.setRegion(RegionUtils.getRegion(region));
        clientsByRegion.put(region, client);
        return client;
    }

    /**
     * Returns a {@link TransferManager} for the given region, or throws an
     * exception when unable. The returned {@link TransferManager} will always
     * be instantiated from whatever {@link AmazonS3} is in the cache,
     * whether provided with {@link #useClient(AmazonS3)} or instantiated
     * automatically from {@link AwsCredentials}.
     *
     * Any {@link TransferManager} returned could be shut down if a new
     * underlying {@link AmazonS3} is provided with
     * {@link #useClient(AmazonS3)}.
     *
     * @param region
     *            The region the returned {@link TransferManager} will be
     *            configured to use.
     * @return A transfer manager for the given region from the cache, or one
     *         instantiated automatically from any existing
     *         {@link AmazonS3},
     */
    public TransferManager getTransferManager(Region region) {
        return getTransferManager(region.toAwsRegion().getName());
    }

    /**
     * Returns a {@link TransferManager} for the given region, or throws an
     * exception when unable. The returned {@link TransferManager} will always
     * be instantiated from whatever {@link AmazonS3} is in the cache,
     * whether provided with {@link #useClient(AmazonS3)} or instantiated
     * automatically from {@link AwsCredentials}.
     *
     * Any {@link TransferManager} returned could be shut down if a new
     * underlying {@link AmazonS3} is provided with
     * {@link #useClient(AmazonS3)}.
     *
     * @param region
     *            The region string the returned {@link TransferManager} will be
     *            configured to use.
     * @return A transfer manager for the given region from the cache, or one
     *         instantiated automatically from any existing
     *         {@link AmazonS3},
     */
    public TransferManager getTransferManager(String region) {
        synchronized (transferManagersByRegion) {
            TransferManager tm = transferManagersByRegion.get(region);
            if (tm == null) {
                tm = new TransferManager(getClient(region));
                transferManagersByRegion.put(region, tm);
            }
            return tm;
        }
    }
}
