/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.format.hessian;

import org.apache.synapse.util.TemporaryData;

import javax.activation.DataSource;
import java.io.*;

/**
 * DataSource which will be used to pass the Hessian messages in to SOAP body within axis2/synapse
 *
 * @see javax.activation.DataSource
 */
public class HessianDataSource implements DataSource {

    /** Content type of the DataSource */
    private String contentType;

    /** Hessian message is kept inside the DataSource as a byte array */
    private TemporaryData data;

    /**
     * Constructs the HessianDataSource from the given InputStream. Inside the HessianDataSource,
     * data is stored in a byte[] or in a temp file format inorder to be able to get the stream any
     * number of time, otherwise the stream can only be read once
     *
	 * @param contentType message content type
	 * @param inputstream contains the Hessian message for later retrieval
	 * @throws IOException failure in reading from the InputStream
	 */
	public HessianDataSource(String contentType, InputStream inputstream) throws IOException {

		this.contentType = contentType;
        this.data = new TemporaryData(4, 1024, "tmp_", ".dat");

        OutputStream out = this.data.getOutputStream();
        byte[] buffer = new byte[1024];
        int c;
        while ((c=inputstream.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
        out.flush();
        out.close();
        inputstream.close();
	}

	public String getContentType() {
		return contentType;
	}

	public InputStream getInputStream() throws IOException {
		return data.getInputStream();
	}

	public String getName() {
		return HessianConstants.HESSIAN_DATA_SOURCE_NAME;
	}

	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("OutputStream can " +
                "not be retrieved from a HessianDataSource");
	}

}
