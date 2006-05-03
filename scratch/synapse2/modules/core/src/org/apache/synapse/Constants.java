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
package org.apache.synapse;

import javax.xml.namespace.QName;


/**
 * <p> Global constants for the Synapse project
 */
public interface Constants {


    String CLASSMEDIATOR = "classmediator";

    QName MEDIATE_OPERATION_NAME = new QName("mediate");

    String MEDIATOR_RESPONSE_PROPERTY = "synapse.mediator.response";
    String MEDIATOR_SYNAPSE_CTX_PROPERTY = "synapse.mediator.environment";

    String ISRESPONSE_PROPERTY = "synapse.isresponse";

    String EMPTYMEDIATOR = "emptymediator";

    //this is for the synapse.config config
    String SYNAPSE_CONFIGURATION = "SynapseConfiguration";

    String SYNAPSE_CONTEXT = "synapse.context";

    String ADD_ADDRESSING = "synapse.send.useaddressing";

    // for security supporting

    String SECURITY_QOS = "synapse_security";
    String ADDRESSING_PROCESSED_CONFIGURATION_CONTEXT = "addressing_processed_configurationContext";

    // addressing properites handling

    String ENGAGE_ADDRESSING_IN_MESSAGE = "__ENGAGE_ADDRESSING_IN_MESSAGE__";

    String ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE = "__ENGAGE_ADDRESSING_OUT_BOUND_MESSAGE__";


}
