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

package org.apache.synapse.config.xml;

import javax.xml.namespace.QName;


import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.commons.schema.XmlSchema;

import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * The Send mediator factory parses a Send element and creates an instance of the mediator
 *
 * //TODO support endpoints, failover and loadbalacing
 *
 * The &lt;send&gt; element is used to send messages out of Synapse to some endpoint. In the simplest case,
 * the place to send the message to is implicit in the message (via a property of the message itself)-
 * that is indicated by the following
 * <pre>
 *  &lt;send/&gt;
 * </pre>
 *
 * If the message is to be sent to one or more endpoints, then the following is used:
 * <pre>
 *  &lt;send&gt;
 *   (endpointref | endpoint)+
 *  &lt;/send&gt;
 * </pre>
 * where the endpointref token refers to the following:
 * <pre>
 * &lt;endpoint ref="name"/&gt;
 * </pre>
 * and the endpoint token refers to an anonymous endpoint defined inline:
 * <pre>
 *  &lt;endpoint address="url"/&gt;
 * </pre>
 * If the message is to be sent to an endpoint selected by load balancing across a set of endpoints,
 * then it is indicated by the following:
 * <pre>
 * &lt;send&gt;
 *   &lt;load-balance algorithm="uri"&gt;
 *     (endpointref | endpoint)+
 *   &lt;/load-balance&gt;
 * &lt;/send&gt;
 * </pre>
 * Similarly, if the message is to be sent to an endpoint with failover semantics, then it is indicated by the following:
 * <pre>
 * &lt;send&gt;
 *   &lt;failover&gt;
 *     (endpointref | endpoint)+
 *   &lt;/failover&gt;
 * &lt;/send&gt;
 * </pre>
 */
public class SendMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(SendMediatorFactory.class);

    private static final QName SEND_Q = new QName(Constants.SYNAPSE_NAMESPACE, "send");

    private static final QName ATT_REF_Q = new QName(Constants.NULL_NAMESPACE, "ref");
    private static final QName ATT_ADDRESS_Q = new QName(Constants.NULL_NAMESPACE, "address");

    private static final String STR_SCHEMA =
        Constants.SCHEMA_PROLOG +
        "\t<xs:element name=\"send\" type=\"send_type\"/>\n" +
        "\t<xs:complexType name=\"send_type\"/>" +
        Constants.SCHEMA_EPILOG;

    private static final XmlSchema SCHEMA =
        org.apache.synapse.config.xml.Util.getSchema(STR_SCHEMA, SEND_Q);

    public Mediator createMediator(OMElement elem) {

        SendMediator sm =  new SendMediator();

        Iterator iter = elem.getChildrenWithName(new QName(Constants.SYNAPSE_NAMESPACE, "endpoint"));
        while (iter.hasNext()) {

            OMElement endptElem = (OMElement) iter.next();
            OMAttribute ref = endptElem.getAttribute(ATT_REF_Q);
            OMAttribute address = endptElem.getAttribute(ATT_ADDRESS_Q);

            Endpoint endpt = new Endpoint();
            if (ref != null) {
                endpt.setRef(ref.getAttributeValue());
            } else if (address != null) {
                try {
                    endpt.setAddress(new URL(address.getAttributeValue()));
                } catch (MalformedURLException e) {
                    String msg = "Invalid endpoint address : " + address.getAttributeValue();
                    log.error(msg, e);
                    throw new SynapseException(msg, e);
                }
            } else {
                String msg = "An endpoint used within a send mediator definition must contain a " +
                    "'ref' (reference) or 'address' (absolute URL) attribute";
                log.error(msg);
                throw new SynapseException(msg);
            }

            sm.addEndpoint(endpt);
        }

        return sm;
    }

    public QName getTagQName() {
        return SEND_Q;
    }

    public XmlSchema getTagSchema() {
        return SCHEMA;
    }
}
