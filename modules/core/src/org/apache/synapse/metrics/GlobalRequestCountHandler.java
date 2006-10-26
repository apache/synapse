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
package org.apache.synapse.metrics;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;

/*
 * This information is published using WS-Management.
 * Access from any location with XXContext.getParameter(Constants.GLOBAL_REQUEST_COUNTER);
 */

public class GlobalRequestCountHandler extends AbstractHandler {

    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        msgContext
                .setProperty(Constants.REQUEST_RECEIVED_TIME, new Long(System.currentTimeMillis()));
        // global increment
        ((Counter) msgContext.getParameter(Constants.GLOBAL_REQUEST_COUNTER).getValue())
                .increment();
        return InvocationResponse.CONTINUE;
    }
}
