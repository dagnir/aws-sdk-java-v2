/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.elasticmapreduce.spi.security;

/**
 * Interface for providing TLS artifacts.
 * Implementations are free to use any strategy for providing TLS artifacts,
 * such as simply providing static artifacts that doesn't change,
 * or more complicated implementations, such as integrating with existing certificates management systems.
 *
 */
public abstract class TLSArtifactsProvider {

  public abstract TLSArtifacts getTlsArtifacts();
}
