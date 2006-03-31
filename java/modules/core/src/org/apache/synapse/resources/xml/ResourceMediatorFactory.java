package org.apache.synapse.resources.xml;

import org.apache.synapse.xml.Constants;
import org.apache.synapse.xml.MediatorFactory;
import org.apache.synapse.xml.MediatorFactoryFinder;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
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

public class ResourceMediatorFactory implements MediatorFactory {

    private Log log = LogFactory.getLog(getClass());

    private static final String RESOURCE = "resource";

	private static final QName RESOURCE_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			RESOURCE);

	private static final QName RESOURCE_TYPE_ATT_Q = new QName("type");

	private static final QName RESOURCE_URI_ROOT_ATT_Q = new QName("uri-root");

    public Mediator createMediator(SynapseEnvironment se, OMElement el) {

        ResourceMediator rp = new ResourceMediator();

        this.addPropertiesMediatorsAndURIRoot(se,el,rp);

        OMAttribute type = el.getAttribute(RESOURCE_TYPE_ATT_Q);
		if (type == null) {
			throw new SynapseException(RESOURCE + " must have "
                    + RESOURCE_TYPE_ATT_Q + " attribute: " + el.toString());
		}

		OMAttribute uriRoot = el.getAttribute(RESOURCE_URI_ROOT_ATT_Q);
		if (uriRoot == null) {
			throw new SynapseException(RESOURCE + " must have "
					+ RESOURCE_URI_ROOT_ATT_Q + " attribute: " + el.toString());
		}
        rp.setType(type.getAttributeValue());

        rp.setURIRoot(uriRoot.getAttributeValue());

        return rp;
    }

    public QName getTagQName() {
        return RESOURCE_Q;
    }

    // this methods will give access to SynapseEnvironment's resouces registration


    public void addPropertiesMediatorsAndURIRoot(SynapseEnvironment se, OMElement el,
                                       ResourceMediator m) {
        this.setURIRoot(se,el,m);

        Iterator it = el.getChildElements();
        List mediators = new LinkedList();
        while (it.hasNext()) {
            OMElement child = (OMElement) it.next();
            Mediator mediator=
                    MediatorFactoryFinder.getMediator(se, child);

            if (mediator != null) {
                if (mediator instanceof PropertyMediator)
                    mediators.add(mediator);
                else
                    throw new SynapseException(
                            "List contains an invalid Mediator" +
                                    mediator.getClass().getName());
            } else
                log.info("Unknown child of all" + child.getLocalName());
        }
        m.setList(mediators);

    }

    public void setURIRoot(SynapseEnvironment se, OMElement el, ResourceMediator m) {

		OMAttribute uriRoot = el.getAttribute(RESOURCE_URI_ROOT_ATT_Q);
		if (uriRoot != null) {
            // uri-root has already set
            se.addResourceMediator(uriRoot.getAttributeValue(), m);
            m.setURIRoot(uriRoot.getAttributeValue());
		}
		log.debug("compile "+el.getLocalName()+" with uri-root '"+uriRoot.getAttributeValue() +"' on "+m.getClass());

	}

}
