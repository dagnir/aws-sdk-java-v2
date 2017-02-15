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

package software.amazon.awssdk.services.s3.model.analytics;

import java.io.Serializable;

public class StorageClassAnalysis implements Serializable {

    private StorageClassAnalysisDataExport dataExport;

    /**
     * Returns the container used to describe how data related to the
     * storage class analysis should be exported.
     */
    public StorageClassAnalysisDataExport getDataExport() {
        return dataExport;
    }

    /**
     * Sets the container used to describe how data related to the
     * storage class analysis should be exported.
     */
    public void setDataExport(StorageClassAnalysisDataExport dataExport) {
        this.dataExport = dataExport;
    }

    /**
     * Sets the container used to describe how data related to the
     * storage class analysis should be exported.
     *
     * Returns this object for method chaining.
     */
    public StorageClassAnalysis withDataExport(StorageClassAnalysisDataExport dataExport) {
        setDataExport(dataExport);
        return this;
    }
}
