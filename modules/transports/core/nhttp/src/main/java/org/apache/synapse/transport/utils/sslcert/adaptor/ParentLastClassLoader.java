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
import java.util.regex.Pattern;

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

    /**
     * Matches the bcprov/bcpkix/bcutil jars the OCSP/CRL feature draws classes from, at any
     * pinned version, while excluding the legacy bcprov-jdk15 Apache Rampart brings in transitively.
     */
    private static final Pattern BOUNCYCASTLE_JAR =
            Pattern.compile("bc(prov|pkix|util)-jdk[0-9]+on-.*[.]jar");

    private File[] jarFiles; // Jar files

    //jars stay open for the life of the loader; see readClassBytes
    private final Hashtable<String,JarFile> openJars = new Hashtable<String,JarFile>();

    public ParentLastClassLoader(ClassLoader parent) {
        super(parent);
        String libDir = System.getProperty("synapse.bcprov.lib");
        if (libDir == null) {
            libDir = "lib";
        }

        File dir = new File(libDir);
        File[] matched = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().startsWith("synapse-nhttp-transport") ||
                        BOUNCYCASTLE_JAR.matcher(f.getName()).matches();
            }
        });
        // listFiles yields null when libDir is absent or unreadable.
        jarFiles = matched != null ? matched : new File[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException();
    }

    @Override
    protected synchronized Class<?> loadClass(String className,
                                              boolean resolve) throws ClassNotFoundException {

        try {
            Class result;

            //checks in cached classes
            result = classes.get(className);
            if (result != null) {
                return result;
            }

            byte[] classByte = readClassBytes(className);
            if (classByte != null) {
                result = defineClass(className, classByte, 0, classByte.length, null);
                classes.put(className, result);
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

    /**
     * Reads a class from the first jar that carries it, or returns null if none does. Jars are
     * opened once and kept open, and entries read buffered, since this runs per class, thousands of times.
     */
    private byte[] readClassBytes(String className) {
        String entryName = className.replace(".", "/") + ".class";
        for (File jarFile : jarFiles) {
            try {
                JarFile jar = openJars.get(jarFile.getPath());
                if (jar == null) {
                    jar = new JarFile(jarFile);
                    openJars.put(jarFile.getPath(), jar);
                }

                JarEntry entry = jar.getJarEntry(entryName);
                if (entry == null) {
                    continue;
                }

                InputStream is = jar.getInputStream(entry);
                try {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read = is.read(buffer);
                    while (-1 != read) {
                        byteStream.write(buffer, 0, read);
                        read = is.read(buffer);
                    }
                    return byteStream.toByteArray();
                } finally {
                    is.close();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}

