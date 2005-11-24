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

public class Constants {
	public static final String SPRINGMEDIATOR = "springmediator";

	public static final String SYNAPSE_MEDIATOR_SPRING_BEAN_FACTORY = "synapse.mediator.spring.beanFactory";

	public static final String SYNAPSE_SPRING_MEDIATOR_NAME = "synapse.spring.mediatorName";

	public static final String CLASSMEDIATOR = "classmediator";

	public static final String SYNAPSE_MEDIATOR_CLASS = "synapse.mediator.class";

	public static final String MEDIATE_OPERATION_NAME = "mediate";

	public static final String SYNAPSE_MEDIATOR_XML_BYTES = "synapse.mediator.xml.bytes";

	public static final String BPELMEDIATOR = "bpelmediator";

	public static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";

	public static final String SYNAPSE = "synapse";

	public static final String MEDIATOR = "mediator";

	public static final String STAGE = "stage";

	public static final String TYPE = "type";

	public static final QName STAGE_Q = new QName(SYNAPSE_NAMESPACE, STAGE);

	public static final QName MEDIATOR_Q = new QName(SYNAPSE_NAMESPACE,
			MEDIATOR);

	public static final QName RULE_TYPE_ATT_Q = new QName("", "rule-type");

	public static final QName TYPE_ATT_Q = new QName("", TYPE);

	public static final QName STAGE_NAME_ATT_Q = new QName("", "name");

	public static final QName IN_ORDER_ATTR_Q = new QName("", "order");

	public static final QName OUT_ORDER_ATTR_Q = new QName("", "order");

	public static final String IN = "inphase";

	public static final String OUT = "outphase";

	public static final String MEDIATOR_CONFIGURATION = "synapse.mediator.configuration";

	public static final String MEDIATOR_RESPONSE_PROPERTY = "synapse.mediator.response";

	public static final String ISRESPONSE_PROPERTY = "synapse.isresponse";

	public static final String EMPTYMEDIATOR = "emptymediator";
}
