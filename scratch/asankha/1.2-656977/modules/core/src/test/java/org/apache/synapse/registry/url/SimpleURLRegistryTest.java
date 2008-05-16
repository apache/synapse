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

package org.apache.synapse.registry.url;

import junit.framework.TestCase;
import org.apache.synapse.config.Entry;
import org.apache.synapse.registry.Registry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

public class SimpleURLRegistryTest extends TestCase {

    private static final String FILE = "target/text.xml";
    private static final String TEXT_1 = "<text1 />";
    private static final String TEXT_2 = "<text2 />";

    public void setUp() throws Exception {
        writeToFile(TEXT_1);
    }

    public void testRegistry() throws Exception {
        Registry reg = new SimpleURLRegistry();
        Properties props = new Properties();
        props.put("root", "file:./");
        props.put("cachableDuration", "1500");
        reg.init(props);
        Entry prop = new Entry();
        prop.setType(Entry.REMOTE_ENTRY);
        prop.setKey(FILE);

        // initial load of file from registry
        assertEquals(TEXT_1, reg.getResource(prop).toString());

        // sleep 1 sec
        Thread.sleep(1000);
        assertEquals(TEXT_1, reg.getResource(prop).toString());

        // sleep another 1 sec, has expired in cache, but content hasnt changed
        Thread.sleep(1000);
        assertEquals(TEXT_1, reg.getResource(prop).toString());

        // the renewed cache should be valid for another 1.5 secs
        // change the file now and change next cache duration
        writeToFile(TEXT_2);
        props.put("cachableDuration", "100");
        reg.init(props);
        // still cached content should be available and valid
        assertEquals(TEXT_1, reg.getResource(prop).toString());

        // now sleep ~1 sec, still cache should be valid
        Thread.sleep(800);
        assertEquals(TEXT_1, reg.getResource(prop).toString());

        // sleep another 1 sec.. cache should expire and new content should be loaded
        Thread.sleep(1000);
        assertEquals(TEXT_2, reg.getResource(prop).toString());

        // change content back to original
        writeToFile(TEXT_1);

        // sleep for .5 sec, now the new content should be loaded as new expiry time
        // is .1 sec
        Thread.sleep(500);
        assertEquals(TEXT_1, reg.getResource(prop).toString());
    }

    public void tearDown() throws Exception {
        new File(FILE).delete();
    }

    private void writeToFile(String content) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(FILE)));
        out.write(content);
        out.close();
    }
}
