/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.db;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.xml.DBLookupMediatorFactory;
import org.apache.synapse.mediators.AbstractMediatorTestCase;
import org.apache.synapse.mediators.TestUtils;

import java.sql.SQLException;
import java.io.File;

public class DBLookupMediatorTest extends AbstractMediatorTestCase {

    private static DBLookupMediator lookup;

    public void testLookupMediator1() throws Exception {
        MessageContext synCtx = TestUtils.getTestContext("<dummy><source>5</source></dummy>");
        assertTrue(lookup.mediate(synCtx));
        assertEquals(synCtx.getProperty("targetProp"), "svr1");
        assertEquals(synCtx.getProperty("categoryProp"), "A");
    }

    public void testLookupMediator2() throws Exception {
        MessageContext synCtx = TestUtils.getTestContext("<dummy><source>6</source></dummy>");
        assertTrue(lookup.mediate(synCtx));
        assertEquals(synCtx.getProperty("targetProp"), "svr3");
        assertEquals(synCtx.getProperty("categoryProp"), "B");
    }

    public static Test suite() {
        return new TestSetup(new TestSuite(DBLookupMediatorTest.class)) {

            protected void setUp() throws Exception {

                File temp = File.createTempFile("temp", "delete");
                temp.deleteOnExit();
                String tempPath = temp.getParent();

                lookup = (DBLookupMediator)
                    new DBLookupMediatorFactory().createMediator(createOMElement(
                        "<dblookup xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                            "  <connection>\n" +
                            "    <pool>\n" +
                            "      <driver>org.apache.derby.jdbc.EmbeddedDriver</driver>\n" +
                            "      <url>jdbc:derby:" + tempPath + "/derbyDB;create=true</url>\n" +
                            "      <user>user</user>\n" +
                            "      <password>pass</password>\n" +
                            "      <property name=\"initialsize\" value=\"2\"/>\n" +
                            "      <property name=\"isolation\" value=\"Connection.TRANSACTION_SERIALIZABLE\"/>\n" +
                            "    </pool>\n" +
                            "  </connection>\n" +
                            "  <statement>\n" +
                            "    <sql>select target, category from destinations where source = ? and type = ?</sql>\n" +
                            "    <parameter expression=\"//source\" type=\"INTEGER\"/>\n" +
                            "    <parameter value=\"GOLD\" type=\"VARCHAR\"/>\n" +
                            "    <result name=\"targetProp\" column=\"target\"/>\n" +
                            "    <result name=\"categoryProp\" column=\"2\"/>\n" +
                            "  </statement>\n" +
                            "</dblookup>"
                    ));

                java.sql.Statement s = lookup.getDataSource().getConnection().createStatement();
                try {
                    s.execute("drop table destinations");
                } catch (SQLException ignore) {}
                try {
                    s.execute("create table destinations(target varchar(10), source int, type varchar(10), category varchar(10))");
                } catch (SQLException ignore) {}
                try {
                    s.execute("insert into destinations values ('svr1', 5, 'GOLD', 'A')");
                    s.execute("insert into destinations values ('svr2', 5, 'SILVER', 'A')");
                    s.execute("insert into destinations values ('svr3', 6, 'GOLD', 'B')");
                } catch (SQLException ignore) {}
                s.close();
            }

            protected void tearDown() throws Exception {

            }
        };
    }
}
