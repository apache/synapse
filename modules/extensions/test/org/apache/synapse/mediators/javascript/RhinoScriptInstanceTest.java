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

import junit.framework.TestCase;

/**
 * Tests for the RhinoScript
 */
public class RhinoScriptInstanceTest extends TestCase {
    
    public RhinoScriptInstanceTest() {
        
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testInvokeFunction() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getPetra() {return 'petra';}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        assertEquals("petra", instance.invokeFunction("getPetra", new Object[0]));
    }

    public void testCreateRhinoFunctionInvoker() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getPetra() {return 'petra';}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getPetra");
        assertNotNull(invoker);
        assertEquals("petra", invoker.invoke(new Object[0]));
    }

}