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
package org.apache.axis2.transport.niohttp.impl;

/**
 * TODO clean up!!!
 */
public final class Constants {

    public static final int DEFAULT_CONNECTION_LINGER = -1;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
    public static final int DEFAULT_CONNECTION_UPLOAD_TIMEOUT = 300000;
    public static final int DEFAULT_SERVER_SOCKET_TIMEOUT = 0;
    public static final boolean DEFAULT_TCP_NO_DELAY = true;

    public static final String CRLF = "\r\n";
    public static final byte CR = (byte) '\r';
    public static final byte LF = (byte) '\n';
    public static final byte SP = (byte) ' ';
    public static final String STRING_SP = " ";
    public static final byte HT = (byte) '\t';
    public static final byte COLON = (byte) ':';
    public static final String STRING_COLON = ":";

    public static final String HTTP_10 = "HTTP/1.0";
    public static final String HTTP_11 = "HTTP/1.1";

    public static final String GET = "GET";
    public static final String POST = "POST";

    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_XML = "text/xml";
    public static final String CHUNKED = "chunked";
    public static final String CONNECTION = "Connection";
    public static final String CLOSE = "close";
    public static final String OK = "OK";
    public static final String LOCATION = "Location";
}
