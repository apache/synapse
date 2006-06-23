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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Tests for the RhinoScript
 */
public class RhinoScriptTest extends TestCase {
    
    public RhinoScriptTest() {
        
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSimpleConstructor() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getPetra() {return 'petra';}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        assertEquals("petra", instance.invokeFunction("getPetra", new Object[0]));
    }

    public void testFullConstructor() {
        ClassLoader cl = getClass().getClassLoader();
        Map contexts = new HashMap();
        contexts.put("name", "petra");
        RhinoScript rhinoScript = new RhinoScript("foo", "function getName() {return name;}",contexts , cl);
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        assertEquals("petra", instance.invokeFunction("getName", new Object[0]));
    }

    public void testCreateInstance() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getPetra() {return 'petra';}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        assertNotNull(instance);
    }

    public void testCreateInstanceWithContext() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getName() {return name;}");
        Map contexts = new HashMap();
        contexts.put("name", "petra");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance(contexts);
        assertEquals("petra", instance.invokeFunction("getName", new Object[0]));
    }

    public void testDefaultResponseType() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getX() {return 42;}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        assertEquals("42", instance.invokeFunction("getX", new Object[0]));
    }

    public void testSetResponseType() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getX() {return 42;}");
        rhinoScript.setResponseClass("getX", Integer.class);
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        Object x = instance.invokeFunction("getX", new Object[0]);
        assertTrue(x instanceof Integer);
        assertEquals(new Integer(42), x);
    }

}