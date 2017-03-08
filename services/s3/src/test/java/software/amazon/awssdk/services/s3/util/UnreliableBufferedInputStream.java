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

package software.amazon.awssdk.services.s3.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.util.FakeIoException;

public class UnreliableBufferedInputStream extends BufferedInputStream {
    // True to throw a FakeIOException; false to throw a RuntimeException
    private final boolean isFakeIOException;
    /**
     * Max number of errors that can be triggered.
     */
    private int maxNumberOfErrors = 1;
    /**
     * Current number of errors that have been triggered.
     */
    private int currNumberOfErrors;
    private int position;

    public UnreliableBufferedInputStream(InputStream is) {
        super(is);
        this.isFakeIOException = false;
    }

    public UnreliableBufferedInputStream(InputStream is, boolean isFakeIOException) {
        super(is);
        this.isFakeIOException = isFakeIOException;
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read != -1) {
            position++;
        }
        triggerError();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        triggerError();
        int read = super.read(b, off, len);
        position += read;
        triggerError();
        return read;
    }

    private void triggerError() throws IOException {
        if (currNumberOfErrors >= maxNumberOfErrors) {
            return;
        }

        if (position >= 100) {
            currNumberOfErrors++;
            if (isFakeIOException) {
                throw new FakeIoException("Fake IO error " + currNumberOfErrors
                                          + " on UnreliableFileInputStream");
            } else {
                throw new IOException("Injected IO error " + currNumberOfErrors
                                      + " on UnreliableFileInputStream");
            }
        }
    }

    public int getNumberOfErrors() {
        return maxNumberOfErrors;
    }

    public void setNumberOfErrors(int numberOfErrors) {
        this.maxNumberOfErrors = numberOfErrors;
    }

    public UnreliableBufferedInputStream withNumberOfErrors(int numberOfErrors) {
        this.maxNumberOfErrors = numberOfErrors;
        return this;
    }

    public int getCurrNumberOfErrors() {
        return currNumberOfErrors;
    }
}
