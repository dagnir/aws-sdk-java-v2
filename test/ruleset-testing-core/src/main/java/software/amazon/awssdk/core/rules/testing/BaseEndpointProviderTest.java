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

package software.amazon.awssdk.core.rules.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Supplier;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.core.rules.testing.model.Expect;

public class BaseEndpointProviderTest {
    protected final void verify(EndpointProviderTestCase tc) {
        Expect expect = tc.getExpect();
        Supplier<Endpoint> testMethod = tc.getTestMethod();
        if (expect.error() != null) {
            assertThatThrownBy(testMethod::get).hasMessageContaining(expect.error());
        } else {
            Endpoint e = testMethod.get();
            assertThat(e.url()).isEqualTo(expect.endpoint().url());
        }
    }
}
