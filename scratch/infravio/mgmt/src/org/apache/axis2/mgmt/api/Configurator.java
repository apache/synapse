package org.apache.axis2.mgmt.api;

import org.apache.axis2.om.OMElement;

/**
 * Created by IntelliJ IDEA.
 * User: Mukund
 * Date: Mar 3, 2006
 * Time: 4:12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Configurator {
    public OMElement getConfiguration();
    public void setConfiguration(OMElement val);
}
