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

package org.apache.synapse.transport.utils.sslcert.adaptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The ParentLastClassLoader is needed because the OCSP/CRL feature for synapse uses a dependency
 * jar from bouncyCastle version bcprov-jdk16-1.46.jar. Apache Rampart which is a synapse
 * dependency for WS-Security also uses an older version of a bouncyCastle jar. The JVM loads the
 * older version thus making OCSP/CRL feature fail to function. This is known as Jar Hell in java
 * community. As a workaround, a Parent Last Class Loader is used to load the classes used by
 * OCSP/CRL feature from the relevant jar files. The default class loader hierarchy in Java is
 * Parent First. This custom ParentLastClassLoader overrides the default behaviour. If
 * ParentLastClassLoader could not load a class it will delegate the job to the parent, the system
 * class loader.
 */
public class ParentLastClassLoader extends ClassLoader {

    //used to cache already defined classes
    private static final Hashtable<String,Class> classes = new Hashtable<String,Class>();

    private File[] jarFiles; // Jar files

    public ParentLastClassLoader(ClassLoader parent) {
        super(parent);
        String libDir = System.getProperty("synapse.bcprov.lib");
        if (libDir == null) {
            libDir = "lib";
        }

        File dir = new File(libDir);
        jarFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().startsWith("synapse-nhttp-transport") ||
                        f.getName().equals("bcprov-jdk15on-1.49.jar");
            }
        });
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException();
    }

    @Override
    protected synchronized Class<?> loadClass(String className,
                                              boolean resolve) throws ClassNotFoundException {

        try {
            byte classByte[];
            Class result;

            //checks in cached classes
            result = classes.get(className);
            if (result != null) {
                return result;
            }

            for (File jarFile : jarFiles) {
                try {
                    JarFile jar = new JarFile(jarFile);
                    JarEntry entry = jar.getJarEntry(className.replace(".", "/") + ".class");
                    InputStream is = jar.getInputStream(entry);
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    int nextValue = is.read();
                    while (-1 != nextValue) {
                        byteStream.write(nextValue);
                        nextValue = is.read();
                    }

                    classByte = byteStream.toByteArray();
                    result = defineClass(className, classByte, 0, classByte.length, null);
                    classes.put(className, result);
                    break;
                } catch (Exception ignored) {}
            }

            if (result != null) {
                return result;
            } else {
                throw new ClassNotFoundException("Not found " + className);
            }
        } catch (ClassNotFoundException e) {
            // didn't find it, try the parent
            return super.loadClass(className, resolve);
        }
    }
}

