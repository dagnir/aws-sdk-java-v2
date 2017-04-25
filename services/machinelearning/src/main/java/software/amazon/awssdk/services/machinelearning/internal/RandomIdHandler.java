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

package software.amazon.awssdk.services.machinelearning.internal;

import java.util.UUID;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.services.machinelearning.model.CreateBatchPredictionRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromRDSRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromRedshiftRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateDataSourceFromS3Request;
import software.amazon.awssdk.services.machinelearning.model.CreateEvaluationRequest;
import software.amazon.awssdk.services.machinelearning.model.CreateMLModelRequest;

/**
 * CreateXxx API calls require a unique (for all time!) ID parameter for
 * idempotency. If the user doesn't specify one, fill in a GUID.
 */
public class RandomIdHandler extends RequestHandler2 {

    @Override
    public AmazonWebServiceRequest beforeMarshalling(
            AmazonWebServiceRequest request) {

        if (request instanceof CreateBatchPredictionRequest) {
            CreateBatchPredictionRequest copy =
                    (CreateBatchPredictionRequest) request;

            if (copy.getBatchPredictionId() == null) {
                copy = copy.clone();
                copy.setBatchPredictionId(UUID.randomUUID().toString());
            }

            return copy;
        } else if (request instanceof CreateDataSourceFromRDSRequest) {
            CreateDataSourceFromRDSRequest copy =
                    (CreateDataSourceFromRDSRequest) request;

            if (copy.getDataSourceId() == null) {
                copy = copy.clone();
                copy.setDataSourceId(UUID.randomUUID().toString());
            }

            return copy;
        } else if (request instanceof CreateDataSourceFromRedshiftRequest) {
            CreateDataSourceFromRedshiftRequest copy =
                    (CreateDataSourceFromRedshiftRequest) request;

            if (copy.getDataSourceId() == null) {
                copy = copy.clone();
                copy.setDataSourceId(UUID.randomUUID().toString());
            }

            return copy;
        } else if (request instanceof CreateDataSourceFromS3Request) {
            CreateDataSourceFromS3Request copy =
                    (CreateDataSourceFromS3Request) request;

            if (copy.getDataSourceId() == null) {
                copy = copy.clone();
                copy.setDataSourceId(UUID.randomUUID().toString());
            }

            return copy;
        } else if (request instanceof CreateEvaluationRequest) {
            CreateEvaluationRequest copy =
                    (CreateEvaluationRequest) request;

            if (copy.getEvaluationId() == null) {
                copy = copy.clone();
                copy.setEvaluationId(UUID.randomUUID().toString());
            }

            return copy;
        } else if (request instanceof CreateMLModelRequest) {
            CreateMLModelRequest copy = (CreateMLModelRequest) request;

            if (copy.getMLModelId() == null) {
                copy = copy.clone();
                copy.setMLModelId(UUID.randomUUID().toString());
            }

            return copy;
        }

        return request;
    }

    @Override
    public void beforeRequest(Request<?> request) {
    }

    @Override
    public void afterResponse(Request<?> request, Response<?> response) {
    }

    @Override
    public void afterError(Request<?> request, Response<?> response, Exception e) {
    }
}
