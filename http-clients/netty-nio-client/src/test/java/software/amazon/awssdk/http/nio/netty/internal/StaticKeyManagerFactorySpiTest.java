/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import java.util.Objects;
import java.util.stream.Stream;
import javax.net.ssl.KeyManager;
import org.junit.Test;

/**
 * Tests for {@link StaticKeyManagerFactorySpi}.
 */
public class StaticKeyManagerFactorySpiTest {

    @Test
    public void constructorCreatesArrayCopy() {
        KeyManager[] keyManagers = new KeyManager[8];
        for (int i = 0; i < keyManagers.length; ++i) {
            keyManagers[i] = mock(KeyManager.class);
        }

        StaticKeyManagerFactorySpi spi = new StaticKeyManagerFactorySpi(keyManagers);

        for (int i = 0; i < keyManagers.length; ++i) {
            keyManagers[i] = null;
        }

        assertThat(Stream.of(spi.engineGetKeyManagers()).anyMatch(Objects::isNull)).isFalse();
    }
}
