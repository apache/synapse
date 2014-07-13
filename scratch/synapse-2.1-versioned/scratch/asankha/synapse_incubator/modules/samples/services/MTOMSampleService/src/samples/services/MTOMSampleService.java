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
package samples.services;

import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMElement;

import javax.activation.DataHandler;
import java.io.*;

public class MTOMSampleService {

    private static final int BUFFER = 2048;

    public void uploadFileUsingMTOM(OMElement element) throws Exception {

        OMText binaryNode = (OMText) element.getFirstOMChild();
        DataHandler dataHandler = (DataHandler) binaryNode.getDataHandler();
        InputStream is = dataHandler.getInputStream();

        File tempFile = File.createTempFile("mtom-", ".jpg");
        FileOutputStream fos = new FileOutputStream(tempFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

        byte data[] = new byte[BUFFER];
        int count;
        while ((count = is.read(data, 0, BUFFER)) != -1) {
            dest.write(data, 0, count);
        }

        dest.flush();
        dest.close();
        System.out.println("Wrote to file : " + tempFile.getAbsolutePath());
    }
}
