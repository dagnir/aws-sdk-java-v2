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

package software.amazon.awssdk.services.dynamodbv2.xspec;

import software.amazon.awssdk.annotation.Immutable;

@Immutable
final class NamedElement extends PathElement {
    private final String name;

    NamedElement(String name) {
        if (name == null) {
            throw new NullPointerException("element");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("element cannot be empty");
        }
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NamedElement)) {
            return false;
        }

        return name.equals(((NamedElement) obj).name);
    }

    @Override
    String asNestedPath() {
        return "." + name;
    }

    @Override
    String asToken(SubstitutionContext context) {
        return context.nameTokenFor(name);
    }

    @Override
    String asNestedToken(SubstitutionContext context) {
        return "." + context.nameTokenFor(name);
    }
}
