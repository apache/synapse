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

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * Tests for the RhinoScript
 */
public class RhinoFunctionInvokerTest extends TestCase {
    
    public RhinoFunctionInvokerTest() {
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testNoArgsInvoke() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getPetra() {return 'petra';}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getPetra");
        assertNotNull(invoker);
        assertEquals("petra", invoker.invoke(null));
    }

    public void testOneArgInvoke() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getS(s) {return s;}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getS");
        assertNotNull(invoker);
        assertEquals("petra", invoker.invoke(new Object[]{"petra"}));
    }

    public void testMultiArgsInvoke() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function concat(x, y) {return x + y}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("concat");
        assertNotNull(invoker);
        assertEquals("petrasue", invoker.invoke(new Object[] { "petra", "sue"}));
    }

    public void testNoResponseInvoke() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getNull() {}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getNull");
        assertNotNull(invoker);
        assertEquals(null, invoker.invoke(new Object[0]));
    }
    
    public void testNullResponseInvoke() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getNull() {return null;}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getNull");
        assertNotNull(invoker);
        assertEquals(null, invoker.invoke(new Object[0]));
    }
    
    public void testResponseTypeDefaultString() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getTrue() {return true;}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getTrue");
        assertNotNull(invoker);
        Object o = invoker.invoke(new Object[0]);
        assertTrue(o instanceof String);
        assertEquals( "true", o);
    }

    public void testResponseTypeBoolean() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getTrue() {return true;}");
        rhinoScript.setResponseClass("getTrue", Boolean.class);
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getTrue");
        assertNotNull(invoker);
        assertTrue(((Boolean)invoker.invoke(new Object[0])).booleanValue());
    }

    public void testResponseTypeStringArray() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getAs() {var as = new Array(1);as[0]='petra';return as;}");
        rhinoScript.setResponseClass("getAs", new String[0].getClass());
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getAs");
        assertNotNull(invoker);
        Object o = invoker.invoke(new Object[0]);
        assertNotNull(o);
        assertTrue(o.getClass().isArray());
        assertEquals("petra", ((Object[])o)[0]);
    }


    public void testResponseTypeBooleanArray() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getBs() {var bs = new Array(1);bs[0]=true;return bs;}");
        rhinoScript.setResponseClass("getBs", new Boolean[0].getClass());
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getBs");
        assertNotNull(invoker);
        Object o = invoker.invoke(new Object[0]);
        assertNotNull(o);
        assertTrue(o.getClass().isArray());
        assertTrue((((Boolean[])o)[0]).booleanValue());
    }

    public void testRequestCustomType() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getFooS(foo) {return foo.getS();}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getFooS");
        assertNotNull(invoker);
        
        Foo foo = new Foo();
        foo.setS("petra");
        Object o = invoker.invoke(new Object[] {foo});
        assertEquals(foo.getS(), o);
    }

    public void testResponseCustomType() {
        RhinoScript rhinoScript = new RhinoScript("foo", "importClass(Packages.org.apache.synapse.mediators.javascript.Foo);function getFoo(s) {var foo = new Foo(); foo.setS(s);return foo;}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getFoo");
        assertNotNull(invoker);
        
        Object o = invoker.invoke(new Object[] {"petra"});
        assertNotNull(o);
        assertEquals("petra", ((Foo)o).getS());
    }

    public void testXMLRequest() throws XmlException, IOException {
        RhinoScript rhinoScript = new RhinoScript("foo", "function isXML(x) {return 'xml' == (typeof x);}");
        rhinoScript.setResponseClass("isXML", Boolean.class);
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("isXML");
        assertNotNull(invoker);

        Object xml =  XmlObject.Factory.parse("<a><b/></a>");
        assertTrue(((Boolean) invoker.invoke(new Object[]{xml})).booleanValue());

        Object notXML = "notXML";
        assertFalse(((Boolean) invoker.invoke(new Object[]{notXML})).booleanValue());
    }
    
    public void testXMLResponse() {
        RhinoScript rhinoScript = new RhinoScript("foo", "function getXML(s) {return <a> { s } </a>;}");
        RhinoScriptInstance instance = rhinoScript.createRhinoScriptInstance();
        RhinoFunctionInvoker invoker = instance.createRhinoFunctionInvoker("getXML");
        assertNotNull(invoker);

        Object xml = invoker.invoke(new Object[]{"petra"});
        assertNotNull(xml);
        assertTrue(xml instanceof XmlObject);
        assertEquals("<a>petra</a>",((XmlObject)xml).toString());
    }

}
