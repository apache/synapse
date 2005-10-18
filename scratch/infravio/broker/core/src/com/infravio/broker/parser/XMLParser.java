package com.infravio.broker.parser;

import noNamespace.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;


public class XMLParser {

    BrokerConfiguratorDocument brokerDoc;

    public XMLParser() {

        brokerDoc = OSBrokerConstants.BrokerDoc;
    }

    public String getActiveAppServer() {
        String serverName = null;
        AppServerDocument.AppServer[] appServer = brokerDoc.getBrokerConfigurator().getConfig().getAppServerArray();
        int len = appServer.length;
        for (int i = 0; i < len; i++) {
            if (appServer[i].getActive().equalsIgnoreCase("true"))
                return appServer[i].getName();
        }
        return serverName;
    }

    /*
     * public DBContext getDBContext() { //To get DataBase related stuff }
     */

    public Collection getEngagedModules(String ServiceName) {
        Collection col = new HashSet();
        ServiceDocument.Service[] service = brokerDoc.getBrokerConfigurator().getServiceMapper().getServiceArray();
        int len = service.length;
        for (int i = 0; i < len; i++) {
            if (service[i].getName().equals(ServiceName)) {
                ModuleDocument.Module[] module = service[i].getModules().getModuleArray();
                int moduleLen = module.length;
                for (int j = 0; j < moduleLen; j++) {
                    if (module[j].getActive().equalsIgnoreCase("true")) {
                        col.add(module[j].getName());
                    }
                }
            }
        }
        return col;
    }

    /*
     * public JMSContext getJMSContext() { //To get JMS related stuff }
     */

    /*
     * public JMXContext getJMXContext() { //To get JMX related stuff }
     */

    public String getManageabilityReference() {

        String ManageabilityReference = brokerDoc.getBrokerConfigurator().getWsdmOutOfBand().getUrlManagebilityReference().getUrl().toString();
        return ManageabilityReference;
    }

    public Collection getServices() {
        Collection col = new HashSet();
        ServiceDocument.Service[] service = brokerDoc.getBrokerConfigurator().getServiceMapper().getServiceArray();
        int len = service.length;
        for (int i = 0; i < len; i++) {
            col.add(service[i].getName());
        }
        return col;
    }

    public int getURLCount(String ServiceName) {
        HashMap col = getURLs(ServiceName);
        return col.size();
    }

    public HashMap getURLs(String ServiceName) {
        HashMap col = new HashMap();
        ServiceDocument.Service[] service = brokerDoc.getBrokerConfigurator().getServiceMapper().getServiceArray();
        int len = service.length;
        for (int i = 0; i < len; i++) {
            if (service[i].getName().equals(ServiceName)) {
                UrlDocument.Url[] url = service[i].getEndPointURL().getUrlArray();
                int urlLen = url.length;
                for (int j = 0; j < urlLen; j++) {
                    int startUrl = url[j].xmlText().indexOf(">");
                    int endUrl = url[j].xmlText().indexOf("<", startUrl);
                    col.put(url[j].xmlText().subSequence(startUrl + 1, endUrl), url[j].getPriority());
                }
                break;
            }
        }
        return col;
    }

    public String getWSDLLocation(String ServiceName) {
        String WSDLLocation = null;
        ServiceDocument.Service[] service = brokerDoc.getBrokerConfigurator().getServiceMapper().getServiceArray();
        int len = service.length;
        for (int i = 0; i < len; i++) {
            if (service[i].getName().equals(ServiceName)) {
                WSDLLocation = service[i].getWsdlLocation().toString();
            }
            break;
        }
        return WSDLLocation;
    }

    public boolean isWSDMManaged() {
        String isWSDM = brokerDoc.getBrokerConfigurator().getWsdmOutOfBand().getManaged();
        if (isWSDM.equalsIgnoreCase("true"))
            return true;
        else
            return false;
    }
}