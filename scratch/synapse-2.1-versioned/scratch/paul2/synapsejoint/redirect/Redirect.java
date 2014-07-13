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
package redirect;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.mediator.Mediator;
import org.apache.synapse.mediator.MediatorException;

public class Redirect implements Mediator {
    public boolean mediate(MessageContext mc) throws MediatorException {
		String uri="http://64.124.140.30:9090/soap";
        System.out.println("Redirect.mediate: "+uri);

        mc.setTo(new EndpointReference(uri));
        return true;
    }
}