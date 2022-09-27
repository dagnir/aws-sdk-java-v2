/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.rules;

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class SigV4AuthScheme implements EndpointAuthScheme {
    private final String signingRegion;
    private final String signingName;
    private final Boolean disableDoubleEncoding;

    private SigV4AuthScheme(Builder b) {
        this.signingRegion = b.signingRegion;
        this.signingName = b.signingName;
        this.disableDoubleEncoding = b.disableDoubleEncoding == null ? false : b.disableDoubleEncoding;
    }

    @Override
    public String name() {
        return "sigv4";
    }

    public String signingRegion() {
        return signingRegion;
    }

    public String signingName() {
        return signingName;
    }

    public Boolean disableDoubleEncoding() {
        return disableDoubleEncoding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String signingRegion;
        private String signingName;
        private Boolean disableDoubleEncoding;

        public Builder signingRegion(String signingRegion) {
            this.signingRegion = signingRegion;
            return this;
        }

        public Builder signingName(String signingName) {
            this.signingName = signingName;
            return this;
        }

        public Builder disableDoubleEncoding(Boolean disableDoubleEncoding) {
            this.disableDoubleEncoding = disableDoubleEncoding;
            return this;
        }

        public SigV4AuthScheme build() {
            return new SigV4AuthScheme(this);
        }
    }
}
