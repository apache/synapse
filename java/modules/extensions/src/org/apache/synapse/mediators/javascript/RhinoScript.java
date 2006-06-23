/**
 *
 *  Copyright 2005 The Apache Software Foundation or its licensors, as applicable.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.synapse.mediators.javascript;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * Represents a compiled JavaScript script. 
 * From this invocable instances of the script can be obtained with the
 * createRhinoScriptInstance methods.
 * 
 * This is a copy of the same class in the Apache Tuscany project
 * with the only change being to remove the JDK5 generics. Please
 * lets try to keep the two classes in sync until a better way is 
 * found to share the code.
 */
public class RhinoScript {

    protected String scriptName;

    protected String script;

    protected Scriptable scriptScope;

    protected Map responseClasses;

    /*
     * Enable dynamic scopes so a script can be used concurrently with a global shared scope and individual execution scopes. See
     * http://www.mozilla.org/rhino/scopes.html 
     */
    private static class MyFactory extends ContextFactory {
        protected boolean hasFeature(Context cx, int featureIndex) {
            if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
                return true;
            }
            return super.hasFeature(cx, featureIndex);
        }
    }

    static {
        ContextFactory.initGlobal(new MyFactory());
    }

    /**
     * Create a new RhinoScript.
     * 
     * @param scriptName
     *            the name of the script. Can be anything, only used in messages to identify the script
     * @param script
     *            the complete script
     */
    public RhinoScript(String scriptName, String script) {
        this(scriptName, script, (Map) null, null);
    }

    /**
     * Create a new RhinoInvoker.
     * 
     * @param scriptName
     *            the name of the script. Can be anything, only used in messages to identify the script
     * @param script
     *            the complete script
     * @param context
     *            name-value pairs that are added in to the scope where the script is compiled. May be null. The value objects are made available to
     *            the script by using a variable with the name.
     * @param classLoader
     *            the ClassLoader Rhino should use to locate any user Java classes used in the script
     */
    public RhinoScript(String scriptName, String script, Map context, ClassLoader cl) {
        this.scriptName = scriptName;
        this.script = script;
        this.responseClasses = new HashMap();
        initScriptScope(scriptName, script, context, cl);
    }

    /**
     * Create a new invokeable instance of the script
     * 
     * @return a RhinoScriptInstance
     */
    public RhinoScriptInstance createRhinoScriptInstance() {
        return createRhinoScriptInstance(null);
    }

    /**
     * Create a new invokeable instance of the script
     * 
     * @param context
     *            objects to add to scope of the script instance
     * @return a RhinoScriptInstance
     */
    public RhinoScriptInstance createRhinoScriptInstance(Map context) {
        Scriptable instanceScope = createInstanceScope(context);
        RhinoScriptInstance rsi = new RhinoScriptInstance(scriptScope, instanceScope, context, responseClasses);
        return rsi;
    }

    /**
     * Initialize the Rhino Scope for this script instance
     */
    protected Scriptable createInstanceScope(Map context) {
        Context cx = Context.enter();
        try {

            Scriptable instanceScope = cx.newObject(scriptScope);
            instanceScope.setPrototype(scriptScope);
            instanceScope.setParentScope(null);

            addContexts(instanceScope, context);

            return instanceScope;

        } finally {
            Context.exit();
        }
    }

    /**
     * Create a Rhino scope and compile the script into it
     */
    protected void initScriptScope(String fileName, String scriptCode, Map context, ClassLoader cl) {
        Context cx = Context.enter();
        try {
            if (cl != null) {
                cx.setApplicationClassLoader(cl);
            }
            this.scriptScope = new ImporterTopLevel(cx, true);
            Script compiledScript = cx.compileString(scriptCode, fileName, 1, null);
            compiledScript.exec(cx, scriptScope);
            addContexts(scriptScope, context);

        } finally {
            Context.exit();
        }
    }

    /**
     * Add the context to the scope. This will make the objects available to a script by using the name it was added with.
     */
    protected void addContexts(Scriptable scope, Map contexts) {
        if (contexts != null) {
            for (Iterator i = contexts.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                Object value = contexts.get(name);
                if (value != null) {
                    scope.put(name, scope, Context.toObject(value, scope));
                }
            }
        }
    }

    public String getScript() {
        return script;
    }

    public String getScriptName() {
        return scriptName;
    }

    public Scriptable getScriptScope() {
        return scriptScope;
    }

    public Map getResponseClasses() {
        return responseClasses;
    }

    /**
     * Set the Java type of a response value. JavaScript is dynamically typed so Rhino
     * cannot always work out what the intended Java type of a response should be, for 
     * example should the statement "return 42" be a Java int, or Integer or Double etc.
     * When Rhino can't determine the type it will default to returning a String, using
     * this method enables overriding the Rhino default to use a specific Java type.   
     */
    public void setResponseClass(String functionName, Class responseClasses) {
        this.responseClasses.put(functionName, responseClasses);
    }

}