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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An invokeable instance of a JavaScript script.
 * 
 * This is a copy of the same class in the Apache Tuscany project
 * with the only change being to remove the JDK5 generics. Please
 * lets try to keep the two classes in sync until a better way is 
 * found to share the code.
 */
public class RhinoScriptInstance {

    private Scriptable scriptScope;

    private Scriptable instanceScope;

    private Map responseClasses;

    public RhinoScriptInstance(Scriptable scriptScope, Scriptable instanceScope, Map context, Map responseClasses) {
        this.scriptScope = scriptScope;
        this.instanceScope = instanceScope;
        this.responseClasses = responseClasses;
        if (this.responseClasses == null) {
            this.responseClasses = new HashMap();
        }
        addContexts(instanceScope, context);
    }

    public Object invokeFunction(String functionName, Object[] args) {
        RhinoFunctionInvoker invoker = createRhinoFunctionInvoker(functionName);
        return invoker.invoke(args);
    }

    public RhinoFunctionInvoker createRhinoFunctionInvoker(String functionName) {
        Function function = getFunction(functionName);
        Class responseClass = (Class) responseClasses.get(functionName);
        RhinoFunctionInvoker invoker = new RhinoFunctionInvoker(instanceScope, function, responseClass);
        return invoker;
    }

    /**
     * Add the context to the scope. This will make the objects available to a script by using the name it was added with.
     */
    protected void addContexts(Scriptable scope, Map contexts) {
        if (contexts != null) {
            Context.enter();
            try {
                for (Iterator i = contexts.keySet().iterator(); i.hasNext();) {
                    String name = (String) i.next();
                    Object value = contexts.get(name);
                    if (value != null) {
                        scope.put(name, scope, Context.toObject(value, scope));
                    }
                }
            } finally {
                Context.exit();
            }
        }
    }

    /**
     * Get the Rhino Function object for the named script function
     */
    protected Function getFunction(String functionName) {

        Object handleObj = scriptScope.get(functionName, instanceScope);
        if (UniqueTag.NOT_FOUND.equals(handleObj)) {
            // Bit of a hack so E4X scripts don't need to define a function for every operation 
            handleObj = scriptScope.get("process", instanceScope);
        }
        if (!(handleObj instanceof Function)) {
            throw new RuntimeException("script function '" + functionName + "' is undefined or not a function");
        }

        return (Function) handleObj;
    }
    
    public Scriptable getScope() {
    	return instanceScope;
    }

}
