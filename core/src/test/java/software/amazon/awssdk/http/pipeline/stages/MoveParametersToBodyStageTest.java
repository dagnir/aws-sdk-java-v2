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

package software.amazon.awssdk.http.pipeline.stages;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.HttpMethodName;

public class MoveParametersToBodyStageTest {

    private final MoveParametersToBodyStage sut = new MoveParametersToBodyStage();

    @Test
    public void postRequestsWithNoBodyHaveTheirParametersMovedToTheBody() throws Exception {
        Request<?> request = new DefaultRequest("my-service");
        request.setContent(null);
        request.setHttpMethod(HttpMethodName.POST);
        request.addParameters("key", singletonList("value"));

        Request<?> output = sut.execute(request, requestContext(request));

        assertThat(output.getParameters()).hasSize(0);
        assertThat(output.getHeaders()).containsKey("Content-Length")
                                       .containsEntry("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        assertThat(output.getContent()).isNotNull();
    }

    @Test
    public void nonPostRequestsWithNoBodyAreUnaltered() throws Exception {
        Stream.of(HttpMethodName.values()).filter(m -> !m.equals(HttpMethodName.POST)).forEach(this::nonPostRequestsUnaltered);
    }

    @Test
    public void postWithContentIsUnaltered() throws Exception {
        InputStream content = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
        Request<?> request = new DefaultRequest("my-service");
        request.setContent(content);
        request.setHttpMethod(HttpMethodName.POST);
        request.addParameters("key", singletonList("value"));

        Request<?> output = sut.execute(request, requestContext(request));

        assertThat(output.getParameters()).hasSize(1);
        assertThat(output.getHeaders()).hasSize(0);
        assertThat(output.getContent()).isEqualTo(content);
    }

    @Test
    public void onlyAlterRequestsIfParamsArePresent() throws Exception {
        Request<?> request = new DefaultRequest("my-service");
        request.setContent(null);
        request.setHttpMethod(HttpMethodName.POST);

        Request<?> output = sut.execute(request, requestContext(request));

        assertThat(output.getParameters()).hasSize(0);
        assertThat(output.getHeaders()).hasSize(0);
        assertThat(output.getContent()).isNull();
    }

    private void nonPostRequestsUnaltered(HttpMethodName method) {
        Request<?> request = new DefaultRequest("my-service");
        request.setContent(null);
        request.setHttpMethod(method);
        request.addParameters("key", singletonList("value"));

        Request<?> output = invokeSafely(() -> sut.execute(request, requestContext(request)));

        assertThat(output.getParameters()).hasSize(1);
        assertThat(output.getHeaders()).hasSize(0);
        assertThat(output.getContent()).isNull();
    }

    private RequestExecutionContext requestContext(Request<?> request) {
        return RequestExecutionContext.builder()
                                      .executionContext(ExecutionContext.builder().build())
                                      .request(request)
                                      .requestConfig(RequestConfig.NO_OP)
                                      .build();
    }
}