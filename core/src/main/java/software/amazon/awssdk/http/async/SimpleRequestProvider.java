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

package software.amazon.awssdk.http.async;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Implementation of {@link SdkHttpRequestProvider} that provides all it's data at once. Useful for
 * non streaming operations that are already marshalled into memory.
 */
public class SimpleRequestProvider implements SdkHttpRequestProvider {

    private final ByteBuffer content;
    private final int length;

    public SimpleRequestProvider(SdkHttpFullRequest request) {
        this.content = ByteBuffer.wrap(invokeSafely(() -> IoUtils.toByteArray(request.getContent())));
        this.length = content.limit();
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                s.onNext(content);
                s.onComplete();
            }

            @Override
            public void cancel() {
            }
        });
    }
}
