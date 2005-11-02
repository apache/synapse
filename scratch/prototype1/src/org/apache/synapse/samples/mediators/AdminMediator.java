package org.apache.synapse.samples.mediators;

import org.apache.synapse.mediator.Mediator;
import org.apache.axis2.context.MessageContext;
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
*
*/

public class AdminMediator implements Mediator {
    public boolean mediate(MessageContext messageContext) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addParameter(String key, Object value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getParameter(String key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
