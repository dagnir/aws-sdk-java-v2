/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.s3.internal;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.runtime.transform.Unmarshaller;


/**
 * An XML response handler that can also process an arbitrary number of headers
 * in the response.
 */
public class ResponseHeaderHandlerChain <T> extends S3XmlResponseHandler<T> {

    private final List<HeaderHandler<T>> headerHandlers;
    
    public ResponseHeaderHandlerChain(Unmarshaller<T, InputStream> responseUnmarshaller, HeaderHandler<T>... headerHandlers) {
        super(responseUnmarshaller);
        this.headerHandlers = Arrays.asList(headerHandlers);
    }

    /* (non-Javadoc)
     * @see software.amazon.awssdk.services.s3.internal.S3XmlResponseHandler#handle(software.amazon.awssdk.http.HttpResponse)
     */
    @Override
    public AmazonWebServiceResponse<T> handle(HttpResponse response) throws Exception {
        AmazonWebServiceResponse<T> awsResponse = super.handle(response);
        
        T result = awsResponse.getResult();
        if (result != null) {
            for (HeaderHandler<T> handler : headerHandlers) {
                handler.handle(result, response);
            }
        }
        
        return awsResponse;
    }
}
