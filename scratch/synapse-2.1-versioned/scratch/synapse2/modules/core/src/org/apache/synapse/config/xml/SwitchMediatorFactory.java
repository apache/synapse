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

import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.filters.SwitchMediator;
import org.apache.synapse.mediators.filters.SwitchCaseMediator;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * Constructs a Switch mediator instance from the given XML configuration
 *
 * <switch source="xpath">
 *   <case regex="string">
 *     mediator+
 *   </case>+
 *   <default>
 *     mediator+
 *   </default>?
 * </switch>
 */
public class SwitchMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(SwitchMediatorFactory.class);

    private static final QName SWITCH_Q  = new QName(Constants.SYNAPSE_NAMESPACE, "switch");
    private static final QName CASE_Q    = new QName(Constants.SYNAPSE_NAMESPACE, "case");
    private static final QName DEFAULT_Q = new QName(Constants.SYNAPSE_NAMESPACE, "default");

    public Mediator createMediator(OMElement elem) {

        SwitchMediator switchMediator = new SwitchMediator();
        Iterator iter = elem.getChildrenWithName(CASE_Q);
        while (iter.hasNext()) {
            switchMediator.addCase((SwitchCaseMediator)
                MediatorFactoryFinder.getInstance().getMediator((OMElement) iter.next()));
        }

        iter = elem.getChildrenWithName(DEFAULT_Q);
        while (iter.hasNext()) {
            switchMediator.addCase((SwitchCaseMediator)
                MediatorFactoryFinder.getInstance().getMediator((OMElement) iter.next()));
            break; // add only the *first* default if multiple are specified, ignore rest if any
        }

        return switchMediator;
    }

    public QName getTagQName() {
        return SWITCH_Q;
    }
}
