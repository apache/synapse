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

import org.apache.xmlbeans.XmlObject;
import org.mozilla.javascript.*;
import org.mozilla.javascript.xml.XMLObject;

/**
 * An invoker for a specific function in a JavaScript script
 * 
 * This is a copy of the same class in the Apache Tuscany project
 * with the only change being to remove the JDK5 generics. Please
 * lets try to keep the two classes in sync until a better way is 
 * found to share the code.
 */
public class RhinoFunctionInvoker {

    private Scriptable instanceScope;

    private Function function;

    private Class responseClass;

    public RhinoFunctionInvoker(Scriptable instanceScope, Function function, Class responseClass) {
        this.instanceScope = instanceScope;
        this.function = function;
        this.responseClass = responseClass;
    }

    public Object invoke(Object[] args) {
        Context cx = Context.enter();
        try {

            Object[] jsArgs = toJavaScript(args, instanceScope, cx);
            Object jsResponse = function.call(cx, instanceScope, instanceScope, jsArgs);
            Object response = fromJavaScript(jsResponse);
            return response;

        } finally {
            Context.exit();
        }
    }

    protected Object[] toJavaScript(Object[] arg, Scriptable scope, Context cx) {
        Object[] jsArgs;
        if (arg == null) {
            jsArgs = new Object[0];
        } else if (arg.length == 1 && arg[0] instanceof XmlObject) {
            Object jsXML = cx.getWrapFactory().wrap(cx, scope, (XmlObject) arg[0], XmlObject.class);
            jsArgs = new Object[] { cx.newObject(scope, "XML", new Object[] { jsXML }) };
        } else {
            jsArgs = new Object[arg.length];
            for (int i = 0; i < jsArgs.length; i++) {
                if (arg[i] == null) {
                    jsArgs[i] = null;
                } else {
                    jsArgs[i] = Context.toObject(arg[i], scope);
                }
            }
        }
        return jsArgs;
    }

    protected Object fromJavaScript(Object o) {
        Object response;
        if (Context.getUndefinedValue().equals(o)) {
            response = null;
        } else if (o instanceof XMLObject) {
            // TODO: E4X Bug? Shouldn't need this copy, but without it the outer element gets lost???
            Scriptable jsXML = (Scriptable) ScriptableObject.callMethod((Scriptable) o, "copy", new Object[0]);
            Wrapper wrapper = (Wrapper) ScriptableObject.callMethod(jsXML, "getXmlObject", new Object[0]);
            response = wrapper.unwrap();
        } else if (o instanceof Wrapper) {
            response = ((Wrapper) o).unwrap();
        } else {
            if (responseClass != null) {
                response = Context.jsToJava(o, responseClass);
            } else {
                response = Context.jsToJava(o, String.class);
            }
        }
        return response;
    }

}
