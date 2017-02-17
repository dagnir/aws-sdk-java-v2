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

package software.amazon.awssdk.services.s3.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Container for rules of cross origin configuration.
 */
public class CorsRule implements Serializable {

    private String id;
    private List<AllowedMethods> allowedMethods;
    private List<String> allowedOrigins;
    private int maxAgeSeconds;
    private List<String> exposedHeaders;
    private List<String> allowedHeaders;

    /**
     * Returns the Id of this rule.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of this rule. Rules must be less than 255 alphanumeric
     * characters, and must be unique for a bucket. If you do not assign an
     * ID, one will be generated.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the ID of this rule and returns a reference to this object for
     * method chaining.
     *
     * @see CorsRule#setId(String)
     */
    public CorsRule withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the allowed methods of the rule.
     */
    public void setAllowedMethods(List<AllowedMethods> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    /**
     * Returns the allowed methods of this rule.
     */
    public List<AllowedMethods> getAllowedMethods() {
        return allowedMethods;
    }

    /**
     * Convenience array style method for {@link #setAllowedMethods(List)}
     */
    public void setAllowedMethods(AllowedMethods... allowedMethods) {
        this.allowedMethods = Arrays.asList(allowedMethods);
    }

    /**
     * Sets the allowed methods of this rule and returns a reference to this object for
     * method chaining.
     *
     * @see CorsRule#setAllowedMethods(List)
     */
    public CorsRule withAllowedMethods(List<AllowedMethods> allowedMethods) {
        this.allowedMethods = allowedMethods;
        return this;
    }

    /**
     * Sets the allowed origins of the rule.
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Returns the allowed origins of this rule and returns a reference to this object for
     * method chaining.
     */
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Convenience array style method for {@link #setAllowedOrigins(List)}
     */
    public void setAllowedOrigins(String... allowedOrigins) {
        this.allowedOrigins = Arrays.asList(allowedOrigins);
    }

    /**
     * Sets the allowed origins of this rule and returns a reference to this object for
     * method chaining.
     *
     * @see CorsRule#setAllowedOrigins(List)
     */
    public CorsRule withAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
        return this;
    }

    /**
     * Sets the ID of this rule and returns a reference to this object for
     * method chaining.
     *
     * @see CorsRule#setId(String)
     */
    public int getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    /**
     * Sets the max age in seconds of the rule.
     */
    public void setMaxAgeSeconds(int maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    /**
     * Sets the max age in seconds of this rule and returns a reference to this object for
     * method chaining.
     *
     * @see CorsRule#setMaxAgeSeconds(int)
     */
    public CorsRule withMaxAgeSeconds(int maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
        return this;
    }

    /**
     * Sets the expose headers of the rule.
     */
    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    /**
     * Returns expose headers of this rule and returns a reference to this object for
     * method chaining.
     */
    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    /**
     * Convenience array style method for {@link #setExposedHeaders(List)}
     */
    public void setExposedHeaders(String... exposedHeaders) {
        this.exposedHeaders = Arrays.asList(exposedHeaders);
    }

    /**
     * Sets the exposeHeaders of this rule and returns a reference to this object for
     * method chaining.
     *
     * @see #setExposedHeaders(List)
     */
    public CorsRule withExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
        return this;
    }

    /**
     * Sets the allowed headers for the rule.
     */
    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    /**
     * Returns allowed headers of this rule.
     */
    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    /**
     * Convenience array style method for {@link #setAllowedHeaders(List)}
     */
    public void setAllowedHeaders(String... allowedHeaders) {
        this.allowedHeaders = Arrays.asList(allowedHeaders);
    }

    /**
     * Sets the allowed headers of this rule and returns a reference to this object for
     * method chaining.
     *
     * @see CorsRule#setAllowedHeaders(List)
     */
    public CorsRule withAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
        return this;
    }

    /**
     * Enumeration of names of the all the allowed methods. 
     *
     */
    public static enum AllowedMethods {
        GET("GET"),
        PUT("PUT"),
        HEAD("HEAD"),
        POST("POST"),
        DELETE("DELETE");

        private final String allowedMethod;

        private AllowedMethods(String allowedMethod) {
            this.allowedMethod = allowedMethod;
        }

        public static AllowedMethods fromValue(String allowedMethod) throws IllegalArgumentException {
            for (AllowedMethods method : AllowedMethods.values()) {
                String methodString = method.toString();
                if (methodString != null && methodString.equals(allowedMethod)) {
                    return method;
                }
            }

            throw new IllegalArgumentException(
                    "Cannot create enum from " + allowedMethod + " value!");
        }

        @Override
        public String toString() {
            return allowedMethod;
        }
    }
}
