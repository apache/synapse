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

package org.apache.synapse.transport;

import org.apache.axis2.description.*;
import org.apache.axis2.Constants;
import org.apache.axis2.engine.ListenerManager;
import org.apache.synapse.transport.vfs.VFSTransportListener;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.receivers.RawXMLINOutMessageReceiver;
import org.apache.axis2.receivers.RawXMLINOnlyMessageReceiver;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;
import java.util.Iterator;

/**
 * Base class for transport util servers used in unit testing
 */
public abstract class UtilsTransportServer {

    private ListenerManager listnMgr = null;
    private ConfigurationContext cfgCtx = null;

    public void start(TransportInDescription trpInDesc, TransportOutDescription trpDescOut) throws Exception {

        // create a dummy repository
        File file = makeCleanPath("target/axis2/repository");

        cfgCtx = ConfigurationContextFactory.
            createConfigurationContextFromFileSystem(file.getAbsolutePath());

        // remove http transport
        cfgCtx.getAxisConfiguration().getTransportsIn().remove("http");        

        // start given transport
        listnMgr = new ListenerManager();
        listnMgr.init(cfgCtx);
        cfgCtx.setTransportManager(listnMgr);
        //listnMgr.addListener(trpInDesc, false);

        trpDescOut.getSender().init(cfgCtx, trpDescOut);
        cfgCtx.getAxisConfiguration().addTransportOut(trpDescOut);
        //trpInDesc.getReceiver().init(cfgCtx, trpInDesc);        
        listnMgr.addListener(trpInDesc, false);
        listnMgr.start();
    }

    public void start() throws Exception {
    }
    
    public void stop() throws Exception {
        listnMgr.stop();
    }

    /**
     * Deploy the standard Echo service with the custom parameters passed in
     * @param name the service name to assign
     * @param parameters the parameters for the service
     * @throws Exception
     */
    public void deployEchoService(String name, List parameters) throws Exception {

        AxisService service = new AxisService(name);
        service.setClassLoader(Thread.currentThread().getContextClassLoader());
        service.addParameter(new Parameter(Constants.SERVICE_CLASS, Echo.class.getName()));

        // add operation echoOMElement
        AxisOperation axisOp = new InOutAxisOperation(new QName("echoOMElement"));
        axisOp.setMessageReceiver(new RawXMLINOutMessageReceiver());
        axisOp.setStyle(WSDLConstants.STYLE_RPC);
        service.addOperation(axisOp);
        service.mapActionToOperation(Constants.AXIS2_NAMESPACE_URI + "/echoOMElement", axisOp);

        // add operation echoOMElementNoResponse
        axisOp = new InOutAxisOperation(new QName("echoOMElementNoResponse"));
        axisOp.setMessageReceiver(new RawXMLINOnlyMessageReceiver());
        axisOp.setStyle(WSDLConstants.STYLE_RPC);
        service.addOperation(axisOp);
        service.mapActionToOperation(Constants.AXIS2_NAMESPACE_URI + "/echoOMElementNoResponse", axisOp);

        Iterator iter = parameters.iterator();
        while (iter.hasNext()) {
            service.addParameter((Parameter) iter.next());
        }

        cfgCtx.getAxisConfiguration().addService(service);
    }

    /**
     * Make sure the given path exists by creating it if required
     * @param path
     * @return
     * @throws Exception
     */
    protected File makePath(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new Exception("Couldn't create directory : " + file.getPath());
            }
        }
        return file;
    }

    /**
     * Delete the path if it exists and, create it
     * @param path
     * @return
     * @throws Exception
     */
    protected File makeCleanPath(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new Exception("Couldn't create directory : " + file.getPath());
            }
        } else {
            // delete any existing
            recursivelydelete(file);
            if (file.exists()) {
                throw new Exception("Couldn't delete directory : " + file.getPath());
            }
            if (!file.mkdirs()) {
                throw new Exception("Couldn't create directory : " + file.getPath());
            }
        }
        return file;
    }

    private void recursivelydelete(File file) {

        File[] children = file.listFiles();
        if (children != null && children.length > 0) {
            for (int i=0; i<children.length; i++) {
                recursivelydelete(children[i]);
            }
        }
        file.delete();
    }
}
