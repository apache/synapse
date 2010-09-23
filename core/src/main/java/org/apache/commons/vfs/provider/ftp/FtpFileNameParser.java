/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs.provider.ftp;

import org.apache.commons.vfs.provider.*;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;


/**
 * Implementation for ftp. set default port to 21
 *
 * @author <a href="http://commons.apache.org/vfs/team-list.html">Commons VFS team</a>
 */
public class FtpFileNameParser extends HostFileNameParser {
    private static final FtpFileNameParser INSTANCE = new FtpFileNameParser();

    private static final int PORT = 21;

    public FtpFileNameParser() {
        super(PORT);
    }

    public static FileNameParser getInstance() {
        return INSTANCE;
    }

    public FileName parseUri(final VfsComponentContext context, FileName base, final String fileName)
            throws FileSystemException {
        // FTP URIs are generic URIs as per RFC 2396
        final StringBuffer name = new StringBuffer();

        // Extract the scheme and authority parts
        final Authority auth = extractToPath(fileName, name);

        // Extract the queuString
        String queuString = UriParser.extractQueryString(name);
        if(queuString == null && base instanceof URLFileName){
            queuString = ((URLFileName) base).getQueryString();
        }

        // Decode and normalise the file name
        UriParser.canonicalizePath(name, 0, name.length(), this);
        UriParser.fixSeparators(name);
        FileType fileType = UriParser.normalisePath(name);
        final String path = name.toString();

        return new URLFileName(
                auth.getScheme(),
                auth.getHostName(),
                auth.getPort(),
                getDefaultPort(),
                auth.getUserName(),
                auth.getPassword(),
                path,
                fileType,
                queuString);

    }
}
