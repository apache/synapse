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
package org.apache.synapse.mediators;

import org.jaxen.Function;
import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.function.StringFunction;
import org.apache.synapse.SynapseMessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Iterator;

/**
 * Implements the XPath extension function synapse:get-property(prop-name)
 */
public class GetPropertyFunction implements Function {

    private static final Log log = LogFactory.getLog(GetPropertyFunction.class);

    private SynapseMessageContext synCtx = null;

    public SynapseMessageContext getSynCtx() {
        return synCtx;
    }

    public void setSynCtx(SynapseMessageContext synCtx) {
        this.synCtx = synCtx;
    }

    public Object call(Context context, List args) throws FunctionCallException {
        if (args.isEmpty()) {
            log.warn("Property key value for lookup was not specified");
            return null;
        } else if (synCtx == null) {
            log.warn("Synapse context has not been set for the XPath extension function" +
                "'synapse:get-property(prop-name)'");
            return null;

        } else {
            Navigator navigator = context.getNavigator();
            Iterator iter = args.iterator();
            while (iter.hasNext()) {
                String key = StringFunction.evaluate(iter.next(), navigator);
                // ignore if more than one argument has been specified
                return synCtx.getProperty(key);                
            }
        }
        return null;
    }
}
