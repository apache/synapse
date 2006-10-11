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

public class ResponseStatus {

    public static final ResponseStatus OK =
        new ResponseStatus(200, Constants.OK);
    public static final ResponseStatus MOVED_PERMANENTLY =
        new ResponseStatus(301, "Moved Permanently");
    public static final ResponseStatus NOT_FOUND =
        new ResponseStatus(404, "Not Found");
    public static final ResponseStatus INTERNAL_SERVER_ERROR =
        new ResponseStatus(500, "Internal Server Error");

    private int code;
    private String message;

    public ResponseStatus() {}

    public ResponseStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String toString() {
        return code + Constants.STRING_SP + message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
