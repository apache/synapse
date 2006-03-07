package org.apache.axis2.mgmt.api;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mukund
 * Date: Mar 3, 2006
 * Time: 4:13:23 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Discovery {
    public List getServices();
    public List getConsumers();
    public List getIntermediaries();
    public List getRegistries();
}
