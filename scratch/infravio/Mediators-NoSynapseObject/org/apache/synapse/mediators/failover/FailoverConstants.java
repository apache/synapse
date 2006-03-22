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

package org.apache.synapse.mediators.failover;

public interface FailoverConstants {

    public static final String CFG_FAILOVER_SERVICE = "failoverService";
    public static final String CFG_FAILOVER_ON_TIMEOUT = "timeoutEnabled";
    public static final String CFG_FAILOVER_ON_SOPAFAULT = "soapFaultEnabled";
    //public static final String CFG_FAILOVER_ON_NTWRK_ERROR = "networkErrorEnabled";
    public static final String CFG_FAILOVER_ON_NTWRK_ERROR = "errorEnabled";
    public static final String CFG_PARAM_SERVICE = "service";
    public static final String CFG_PARAM_ACTIVE = "active";
    public static final String CFG_PARAM_PRIMARY = "primary";
    public static final String CFG_PARAM_TIMEOUT = "timeout";
    public static final String CFG_FAILOVER_RESULT = "synapse.failover.result";
    public static final String CFG_FAILOVER_XML = "failover.xml";
    public static final String CFG_XML_FOLDER = "META-INF";
    public static final String CFG_PARAMSET = "parameterSet";
    public static final String TIMEOUT_MSG = "";

}