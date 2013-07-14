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
package org.apache.synapse.rest.version;

import org.apache.synapse.MessageContext;
import org.apache.synapse.config.xml.rest.VersionStrategyFactory;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.RESTUtils;

public class URLBasedVersionStrategy extends AbstractVersionStrategy {
    String versionParam;

    public URLBasedVersionStrategy(API api, String version, String versionParam) {
        super(api, version, VersionStrategyFactory.TYPE_URL);
        this.versionParam = versionParam;
    }

    public boolean isMatchingVersion(Object versionInfoObj) {
        MessageContext msgContext = (MessageContext) versionInfoObj;
        String path = RESTUtils.getFullRequestPath(msgContext);

        String context = getAPI().getContext();
        String pathStringAfterContext = path.substring(context.length());

        //check if context can be invalid for path url and avoid matching a false version string
        if (pathStringAfterContext == null || !pathStringAfterContext.startsWith("/")) {
            return false;
        }

        //path after context will be starting from a "/" and compute index for next immediate "/"
        int indexOfNextSlash = pathStringAfterContext.substring(1).indexOf("/");

        String incomingVersionStr;
        if (indexOfNextSlash != -1) {
            incomingVersionStr = pathStringAfterContext.substring(1, indexOfNextSlash + 1);
        } else {
            String versionStr = pathStringAfterContext.substring(1);
            //assume no slash at the end ie:-GET /context/1.0.0
            incomingVersionStr = versionStr;
            //check special case
            if (versionStr.contains("?")) {
                incomingVersionStr = versionStr.substring(0,versionStr.indexOf("?"));
            }
        }

        return version.equals(incomingVersionStr);
    }

    public String getVersionParam() {
        return versionParam;
    }
}
