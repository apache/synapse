/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.synapse.mediators;

import java.net.URI;
import java.net.URISyntaxException;

public class URIFragments {

    public static final int FULL_URI    = -2;
    public static final int PORT        = -1;

    public static final int PROTOCOL    = 0;
    public static final int USER_INFO   = 1;
    public static final int HOST        = 2;
    public static final int PATH        = 3;
    public static final int QUERY       = 4;
    public static final int REF         = 5;

    private int port = -1;
    private String[] fragments = new String[6];

    public URIFragments() {

    }

    public URIFragments(URI uri) {
        setFragments(uri);
    }

    public void setFragments(URI uri) {
        fragments[PROTOCOL] = uri.getScheme();
        fragments[USER_INFO] = uri.getUserInfo();
        fragments[HOST] = uri.getHost();
        fragments[PATH] = ("".equals(uri.getPath()) ? null : uri.getPath());
        fragments[QUERY] = uri.getQuery();
        fragments[REF] = uri.getFragment();
        port = uri.getPort();
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setStringFragment(int index, String value) {
        fragments[index] = value;
    }

    public String getStringFragment(int index) {
        return fragments[index];
    }

    public URI toURI() throws URISyntaxException {
        return new URI(
                fragments[PROTOCOL],
                fragments[USER_INFO],
                fragments[HOST],
                port,
                fragments[PATH],
                fragments[QUERY],
                fragments[REF]);
    }

    public String toURIString() throws URISyntaxException {
        return toURI().toString();
    }
}
