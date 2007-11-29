/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sandesha2.faulttests;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FaultTestUtils {

	/** 
	 * Sets up a connection to an HTTP endpoint
	 * @return
	 */
	public static HttpURLConnection getHttpURLConnection(String uri, String soapAction) throws Exception {
    // Open a connection to the endpoint
		URL endPointURL = new URL(uri);
		
		HttpURLConnection connection = (HttpURLConnection) endPointURL.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.addRequestProperty("SOAPAction", soapAction);
		connection.setRequestProperty("Content-Type", "text/xml"); 
		 
		connection.connect();

		return connection;
	}
	
	/**
	 * Reads a response from the HttpURLConnection instance
	 * @param connection
	 * @return
	 * @throws IOException 
	 */
	public static final String retrieveResponseMessage(HttpURLConnection connection) {
		InputStream tmpIn2 = null;
		try {
			tmpIn2 = connection.getInputStream();
		} catch (IOException e) {
			tmpIn2 = connection.getErrorStream();
		}

		// Read the sync response
		boolean done = false;

    byte[]      buffer = new byte[4096];
    String      message = null;
    int         saved = 0 ;
    int         len ;
    
    a:
		for (;;) {
			if (done) {
				break;
			}

			len = buffer.length;
			// Used to be 1, but if we block it doesn't matter
			// however 1 will break with some servers, including apache
			if (len == 0) {
				len = buffer.length;
			}
			if (saved + len > buffer.length) {
				len = buffer.length - saved;
			}
			int len1 = 0;

			while (len1 == 0) {
				try {
					len1 = tmpIn2.read(buffer, saved, len);
				} catch (Exception ex) {
					ex.printStackTrace();
					if (done && saved == 0) {
						break a;
					}
					len1 = -1;
					break;
				}
			}
			len = len1;

			if (len == -1 && saved == 0) {
				break;
			}
			if (len == -1) {
				done = true;
			}

			// No matter how we may (or may not) format it, send it
			// on unformatted - we don't want to mess with how its
			// sent to the other side, just how its displayed

			message = new String(buffer, 0, len);

			System.out.println(message);
		}

    return message;
	}
}
