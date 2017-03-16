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

package software.amazon.awssdk.metrics.internal.cloudwatch.provider.transform;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.AwsMetricTransformerFactory;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.Dimensions;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.RequestMetricTransformer;
import software.amazon.awssdk.metrics.spi.MetricType;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.dynamodb.metrics.DynamoDBRequestMetric;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;

/**
 * An internal service provider implementation for an DyanmoDB specific request
 * metric transformer.
 *
 * This class is loaded only if there are Amazon DyanmoDB specific predefined
 * metrics to be processed.
 *
 * @see AwsMetricTransformerFactory
 */
@ThreadSafe
public class DynamoDbRequestMetricTransformer implements RequestMetricTransformer {
    @Override
    public List<MetricDatum> toMetricData(MetricType metricType,
                                          Request<?> request, Response<?> response) {
        try {
            return toMetricData0(metricType, request, response);
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException e) {
            // Ignored or expected.
        } catch (InvocationTargetException e) {
            LogFactory.getLog(getClass()).debug("", e.getCause());
        } catch (Exception e) {
            LogFactory.getLog(getClass()).debug("", e);
        }
        return null;
    }

    private List<MetricDatum> toMetricData0(MetricType metricType,
                                            Request<?> req, Response<?> response) throws SecurityException,
                                                                                         NoSuchMethodException,
                                                                                         IllegalAccessException,
                                                                                         InvocationTargetException {
        if (!(metricType instanceof DynamoDbRequestMetric)) {
            return null;
        }
        // Predefined metrics across all aws http clients
        DynamoDbRequestMetric predefined = (DynamoDbRequestMetric) metricType;
        switch (predefined) {
            case DynamoDBConsumedCapacity:
                if (response == null) {
                    return Collections.emptyList();
                }
                Object awsResponse = response.getAwsResponse();
                Method method = awsResponse.getClass().getMethod("getConsumedCapacity");
                Object value = method.invoke(awsResponse);
                if (!(value instanceof ConsumedCapacity)) {
                    return Collections.emptyList();
                }
                ConsumedCapacity consumedCapacity = (ConsumedCapacity) value;
                Double units = consumedCapacity.getCapacityUnits();
                if (units == null) {
                    return Collections.emptyList();
                }
                String tableName = consumedCapacity.getTableName();
                List<Dimension> dims = new ArrayList<Dimension>();
                dims.add(new Dimension()
                                 .withName(Dimensions.MetricType.name())
                                 .withValue(metricType.name()));
                // request type specific
                dims.add(new Dimension()
                                 .withName(Dimensions.RequestType.name())
                                 .withValue(requestType(req)));
                // table specific
                dims.add(new Dimension()
                                 .withName(DynamoDbDimensions.TableName.name())
                                 .withValue(tableName));
                MetricDatum datum = new MetricDatum()
                        .withMetricName(req.getServiceName())
                        .withDimensions(dims)
                        .withUnit(StandardUnit.Count)
                        .withValue(units);
                return Collections.singletonList(datum);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Returns the name of the type of request.
     */
    private String requestType(Request<?> req) {
        return req.getOriginalRequest().getClass().getSimpleName();
    }
}
