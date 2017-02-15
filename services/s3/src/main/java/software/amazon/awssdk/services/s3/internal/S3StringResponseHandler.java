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

package software.amazon.awssdk.services.s3.internal;

import java.io.InputStream;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.util.StringUtils;

/**
 * Custom S3 response handler for responses that simply contain text data that
 * doesn't need to be parsed as XML.
 */
public class S3StringResponseHandler extends AbstractS3ResponseHandler<String> {

    /* (non-Javadoc)
     * @see software.amazon.awssdk.http.HttpResponseHandler#handle(software.amazon.awssdk.http.HttpResponse)
     */
    public AmazonWebServiceResponse<String> handle(HttpResponse response) throws Exception {
        AmazonWebServiceResponse<String> awsResponse = parseResponseMetadata(response);

        int bytesRead;
        byte[] buffer = new byte[1024];
        StringBuilder builder = new StringBuilder();
        InputStream content = response.getContent();
        while ((bytesRead = content.read(buffer)) > 0) {
            builder.append(new String(buffer, 0, bytesRead, StringUtils.UTF8));
        }
        awsResponse.setResult(builder.toString());

        return awsResponse;
    }

}
