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

/**
 * A {@link AnalyticsFilterPredicate} class to represent the
 * prefix to use when evaluating an analytics filter.
 */
public final class AnalyticsPrefixPredicate extends AnalyticsFilterPredicate {

    private final String prefix;

    public AnalyticsPrefixPredicate(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix to use when evaluating an analytics filter.
     */
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void accept(AnalyticsPredicateVisitor analyticsPredicateVisitor) {
        analyticsPredicateVisitor.visit(this);
    }
}
