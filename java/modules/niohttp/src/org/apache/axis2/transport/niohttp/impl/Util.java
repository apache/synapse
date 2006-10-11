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
package org.apache.axis2.transport.niohttp.impl;

import java.net.SocketException;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Enumeration;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

public class Util {

    /**
     * Copied from transport.http of Axis2
     * <p/>
     * Returns the ip address to be used for the replyto epr
     * CAUTION:
     * This will go through all the available network interfaces and will try to return an ip address.
     * First this will try to get the first IP which is not loopback address (127.0.0.1). If none is found
     * then this will return this will return 127.0.0.1.
     * This will <b>not<b> consider IPv6 addresses.
     * <p/>
     * TODO:
     * - Improve this logic to genaralize it a bit more
     * - Obtain the ip to be used here from the Call API
     *
     * @return Returns String.
     * @throws java.net.SocketException
     */
    public static String getIpAddress() throws SocketException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        String address = "127.0.0.1";

        while (e.hasMoreElements()) {
            NetworkInterface netface = (NetworkInterface) e.nextElement();
            Enumeration addresses = netface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress ip = (InetAddress) addresses.nextElement();
                if (!ip.isLoopbackAddress() && isIP(ip.getHostAddress())) {
                    return ip.getHostAddress();
                }
            }
        }
        return address;
    }

    private static boolean isIP(String hostAddress) {
        return hostAddress.split("[.]").length == 4;
    }

    /**
     * Dumps the given bytes to STDOUT as a hex dump (up to length bytes).
     *
     * @param byteBuffer the data to print as hex
     * @param length     the number of bytes to print
     * @return ...
     */
    public static final String dumpAsHex(byte[] byteBuffer, int length) {
        StringBuffer outputBuf = new StringBuffer(length * 4);

        int p = 0;
        int rows = length / 16;

        for (int i = 0; (i < rows) && (p < length); i++) {
            int ptemp = p;

            for (int j = 0; j < 16; j++) {
                String hexVal = Integer.toHexString(byteBuffer[ptemp] & 0xff);

                if (hexVal.length() == 1) {
                    hexVal = "0" + hexVal; //$NON-NLS-1$
                }

                outputBuf.append(hexVal + " "); //$NON-NLS-1$
                ptemp++;
            }

            outputBuf.append("    "); //$NON-NLS-1$

            for (int j = 0; j < 16; j++) {
                if ((byteBuffer[p] > 32) && (byteBuffer[p] < 127)) {
                    outputBuf.append((char) byteBuffer[p] + ""); //$NON-NLS-1$
                } else {
                    outputBuf.append("."); //$NON-NLS-1$
                }

                p++;
            }

            outputBuf.append("\n"); //$NON-NLS-1$
        }

        int n = 0;

        for (int i = p; i < length; i++) {
            String hexVal = Integer.toHexString(byteBuffer[i] & 0xff);

            if (hexVal.length() == 1) {
                hexVal = "0" + hexVal; //$NON-NLS-1$
            }

            outputBuf.append(hexVal + " "); //$NON-NLS-1$
            n++;
        }

        for (int i = n; i < 16; i++) {
            outputBuf.append("   "); //$NON-NLS-1$
        }

        outputBuf.append("    "); //$NON-NLS-1$

        for (int i = p; i < length; i++) {
            if ((byteBuffer[i] > 32) && (byteBuffer[i] < 127)) {
                outputBuf.append((char) byteBuffer[i] + ""); //$NON-NLS-1$
            } else {
                outputBuf.append("."); //$NON-NLS-1$
            }
        }

        outputBuf.append("\n"); //$NON-NLS-1$

        return outputBuf.toString();
    }

    public static void copyStreams(InputStream in, OutputStream out) throws IOException {
        // Transfer bytes from in to out
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
