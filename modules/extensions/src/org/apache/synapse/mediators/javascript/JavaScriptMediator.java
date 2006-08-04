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
package org.apache.synapse.mediators.javascript;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * A Synapse mediator using a JavaScript function.
 * The function should be named "mediate" and may return true or false, 
 * if the function has no explicit return or returns null then true is the 
 * default. The MessageContext passed in to the function has additional
 * JavaScript helper methods, for example, to aid working with the message
 * payload as E4X XML. See the E4XMessageContext class for details.
 */
public class JavaScriptMediator extends AbstractMediator {

    private RhinoFunctionInvoker mediateFunction;
    private String script;

    public boolean mediate(MessageContext synCtx) {
        Boolean b = (Boolean) mediateFunction.invoke(new Object[] { synCtx });
        return b == null ? true : b.booleanValue(); // default response to true
    }

    public void setScript(String script) {
        this.script = script;
        RhinoScript rhinoScript = new RhinoScript("JavaScriptMediator", script);
        rhinoScript.setResponseClass("mediate", Boolean.class);
        RhinoScriptInstance scriptInstance = rhinoScript.createRhinoScriptInstance();
        this.mediateFunction = scriptInstance.createRhinoFunctionInvoker("mediate");
    }

    public String getScript() {
        return script;
    }
}
