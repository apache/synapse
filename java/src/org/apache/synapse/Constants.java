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
 *
 * 
 * <p> Global constants for the Synapse project
 *
 */
public interface Constants {
	

	String CLASSMEDIATOR = "classmediator";

	QName MEDIATE_OPERATION_NAME = new QName("mediate");

	String MEDIATOR_RESPONSE_PROPERTY = "synapse.mediator.response";
	String MEDIATOR_SYNAPSE_ENV_PROPERTY = "synapse.mediator.environment";

	String ISRESPONSE_PROPERTY = "synapse.isresponse";

	String EMPTYMEDIATOR = "emptymediator";

    //this is for the synapse.xml config
    String SYNAPSECONFIGURATION = "SynapseConfiguration";

	String SYNAPSE_ENVIRONMENT = "synapse.environment";
    // for the mediator return value
    String MEDIATOR_STATUS="mediator.status";
}
