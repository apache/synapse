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
package org.apache.synapse.core.axis2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.ServerManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * When Synapse is deployed on a WAR container, this is the init servlet that kicks off the
 * Synapse instance, calling on the ServerManager
 */

public class SynapseStartUpServlet extends HttpServlet {

    private static Log log = LogFactory.getLog(SynapseStartUpServlet.class);
    private static final String ALREADY_INITED = "synapseAlreadyInited";

    public void init() throws ServletException {
    	ServletConfig servletConfig = getServletConfig();
    	ServletContext servletContext = servletConfig.getServletContext();
        if (Boolean.TRUE.equals(servletContext.getAttribute(ALREADY_INITED))) {
            return;
        }

        ServerManager serverManager = ServerManager.getInstance();
        String synHome = loadParameter(servletConfig, SynapseConstants.SYNAPSE_HOME, false);

        if (synHome == null) {
            log.info("synapse.home not set; using web application root as default value");
            String webinfPath = servletContext.getRealPath("WEB-INF");
            if (webinfPath == null || !webinfPath.endsWith("WEB-INF")) {
                handleException("Unable to determine web application root directory");
            } else {
                synHome = webinfPath.substring(0, webinfPath.length()-7);
                log.info("Setting synapse.home to : " + synHome);
            }
        }
        serverManager.setSynapseHome(synHome);
        
        serverManager.setSynapseXMLPath(loadParameter(servletConfig, SynapseConstants.SYNAPSE_XML, true));
        String resolveRoot = loadParameter(servletConfig, SynapseConstants.RESOLVE_ROOT, false);
        if (resolveRoot != null) {
            serverManager.setResolveRoot(resolveRoot);
        }
        serverManager.setAxis2Repolocation(loadParameter(servletConfig, org.apache.axis2.Constants.AXIS2_REPO, true));
        serverManager.setAxis2Xml(loadParameter(servletConfig, org.apache.axis2.Constants.AXIS2_CONF, true));
        serverManager.setServerName(loadParameter(servletConfig, SynapseConstants.SERVER_NAME, false));

        serverManager.start();
        servletContext.setAttribute(ALREADY_INITED, Boolean.TRUE);
    }


    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
    }

    public void destroy() {
        try {
            ServerManager serverManager = ServerManager.getInstance();
            serverManager.stop();
            getServletContext().removeAttribute(ALREADY_INITED);
        } catch (Exception e) {
            log.error("Error stopping the Synapse listener manager", e);
        }
    }

    private String loadParameter(ServletConfig servletConfig, String name, boolean required)
        throws ServletException {

        if (System.getProperty(name) == null) {

            String value = servletConfig.getInitParameter(name);
            log.debug("Init parameter '" + name + "' : " + value);

            if ((value == null || value.trim().length() == 0) && required) {
                handleException("A valid system property or init parameter '" + name + "' is required");
            } else {
                return value;
            }
        } else {
            return System.getProperty(name);
        }
        return null;
    }

    private void handleException(String message) throws ServletException {
        log.error(message);
        log(message);
        throw new ServletException(message);
    }
}
