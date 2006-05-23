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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Util;
import org.apache.synapse.mediators.transform.TransformMediator;
import org.apache.synapse.api.Mediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Creates a transform mediator from the given XML
 *
 * <pre>
 * &lt;transform xslt|xquery="url" [source="xpath"]&gt;
 *   &lt;property name="string" (value="literal" | expression="xpath")/&gt;*
 * &lt;/transform&gt;
 * </pre>
 */
public class TransformMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(TransformMediatorFactory.class);
    private static final QName LOG_Q    = new QName(Constants.SYNAPSE_NAMESPACE, "transform");

    public QName getTagQName() {
        return LOG_Q;
    }

    public Mediator createMediator(OMElement elem) {

        TransformMediator transformMediator = new TransformMediator();

        OMAttribute attXslt   = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "xslt"));
        OMAttribute attXQuery = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "xquery"));
        OMAttribute attSource = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "source"));

        if (attXslt != null) {
            try {
                transformMediator.setXsltUrl(new URL(attXslt.getAttributeValue()));
            } catch (MalformedURLException e) {
                String msg = "Invalid URL specified for the xslt attribute : " + attXslt.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg);
            }

        } else  if (attXQuery != null) {
            try {
                transformMediator.setXQueryUrl(new URL(attXQuery.getAttributeValue()));
            } catch (MalformedURLException e) {
                String msg = "Invalid URL specified for the xquery attribute : " + attXQuery.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg);
            }

        } else {
            String msg = "The 'xslt' or 'xquery' attributes are required for the Transform mediator";
            log.error(msg);
            throw new SynapseException(msg);
        }

        if (attSource != null) {
            try {
                AXIOMXPath xp = new AXIOMXPath(attSource.getAttributeValue());
                Util.addNameSpaces(xp, elem, log);
                transformMediator.setSource(xp);

            } catch (JaxenException e) {
                String msg = "Invalid XPath specified for the source attribute : " + attSource.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        transformMediator.addAllProperties(MediatorPropertyFactory.getMediatorProperties(elem));

        return transformMediator;
    }

}