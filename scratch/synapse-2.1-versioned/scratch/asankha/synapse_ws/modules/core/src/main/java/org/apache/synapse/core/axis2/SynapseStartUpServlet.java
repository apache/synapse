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
import java.io.File;
import java.io.IOException;

/**
 * This servlet will start and stop all the listeners.
 */

public class SynapseStartUpServlet extends HttpServlet {

    private static Log log = LogFactory.getLog(SynapseStartUpServlet.class);

    public void init() throws ServletException {
        super.init();
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        ServerManager serverManager = ServerManager.getInstance();
        ServletContext servletContext = servletConfig.getServletContext();
        if ("true".equals(servletContext.getAttribute("hasAlreadyInit"))) {
            return;
        }
        String synapseHome = resolveSynapseHome(servletConfig);
        //Setting the all required system properties
        if (synapseHome != null) {
            if (synapseHome.endsWith("/")) {
                synapseHome = synapseHome.substring(0, synapseHome.lastIndexOf("/"));
            }
            System.setProperty(SynapseConstants.SYNAPSE_HOME, synapseHome);
            //setting axis2 repository location
            String axis2Repo = System.getProperty(org.apache.axis2.Constants.AXIS2_REPO);
            if (axis2Repo == null) {
                ServerManager.getInstance().setAxis2Repolocation(synapseHome + "/WEB-INF" +
                    File.separator + "repository");
                System.setProperty(org.apache.axis2.Constants.AXIS2_REPO,
                    synapseHome + "/WEB-INF" +
                        File.separator + "repository");
            }
            //setting axis2 configuration location
            String axis2Xml = System.getProperty(org.apache.axis2.Constants.AXIS2_CONF);
            if (axis2Xml == null) {
                System.setProperty(org.apache.axis2.Constants.AXIS2_CONF,
                    synapseHome + File.separator
                        + "WEB-INF/conf"
                        + File.separator + org.apache.axis2.Constants.AXIS2_CONF);
            }
            //setting synapse configuration location
            String synapseXml = System.getProperty(org.apache.synapse.SynapseConstants.SYNAPSE_XML);
            if (synapseXml == null) {
                System.setProperty(org.apache.synapse.SynapseConstants.SYNAPSE_XML,
                    synapseHome + File.separator
                        + "WEB-INF/conf"
                        + File.separator + org.apache.synapse.SynapseConstants.SYNAPSE_XML);

            }
        } else {
            log.fatal("Can not resolve synapse home  : startup failed");
            return;
        }
        serverManager.start();
        servletContext.setAttribute("hasAlreadyInit", "true");
    }


    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException,
        IOException {
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException,
        IOException {
    }

    public void destroy() {
        try {
            ServerManager serverManager = ServerManager.getInstance();
            serverManager.stop(); // will stop all started  listeners
        } catch (Exception ignored) {
        }
    }

    private String resolveSynapseHome(ServletConfig servletConfig) {
        // If synapse.home has provided as init-param,the it will take as synapse home
        String synapseHomeAsParam = servletConfig.getInitParameter(SynapseConstants.SYNAPSE_HOME);
        if (synapseHomeAsParam != null) {
            if (synapseHomeAsParam.endsWith("/")) {
                return synapseHomeAsParam.substring(0, synapseHomeAsParam.lastIndexOf("/"));
            }
        }
        //if synapse.home has set as a system property , then use it
        String synapseHome = System.getProperty(SynapseConstants.SYNAPSE_HOME);
        //Setting the all required system properties
        if (synapseHome == null || "".equals(synapseHome)) {
            ServletContext servletContext = servletConfig.getServletContext();
            //if synapse.home stil can not find ,then resolve it using real path of the WEB-INF
            String webinfPath = servletContext.getRealPath("WEB-INF");
            if (webinfPath != null) {
                synapseHome = webinfPath.substring(0, webinfPath.lastIndexOf("WEB-INF"));
                if (synapseHome != null) {
                    if (synapseHome.endsWith("/")) {
                        synapseHome = synapseHome.substring(0, synapseHome.lastIndexOf("/"));
                    }
                }
            }
        }
        return synapseHome;
    }
}
