package org.apache.axis2.mgmt.api;

import org.apache.axis2.om.OMElement;

/**
 * Created by IntelliJ IDEA.
 * User: Mukund
 * Date: Mar 3, 2006
 * Time: 4:03:14 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Sampler {

    public int getState();

    public void start();
    public void stop();
    public void pause();
    public void restart();

    public void setLogLevel(int level);
    public int getLogLevel();

    public void ping(OMElement info);

}
