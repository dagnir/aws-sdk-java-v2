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

package software.amazon.awssdk.services.s3.model.metrics;

import software.amazon.awssdk.services.s3.model.Tag;

/**
 * A {@link MetricsFilterPredicate} class to represent the {@link Tag} object
 * to use when evaluating a metrics filter.
 */
public final class MetricsTagPredicate extends MetricsFilterPredicate {

    private final Tag tag;

    public MetricsTagPredicate(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public void accept(MetricsPredicateVisitor metricsPredicateVisitor) {
        metricsPredicateVisitor.visit(this);
    }
}
