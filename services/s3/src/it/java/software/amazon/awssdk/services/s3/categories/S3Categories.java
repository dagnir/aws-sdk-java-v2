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

package software.amazon.awssdk.services.s3.categories;

/**
 * Junit Test Categories for the S3 Client. Categories are just marker interfaces to group tests
 * under common criteria like fast or slow, unit or integration, feature a or feature b, etc
 */
public class S3Categories {

    /**
     * Marker category for slow tests. Slow is roughly defined as greater than 30 seconds and less
     * than five minutes
     */
    public interface Slow {
    }

    /**
     * Marker category for really slow tests. ReallySlow is roughly defined as greater than five
     * minutes
     */
    public interface ReallySlow {
    }
}
