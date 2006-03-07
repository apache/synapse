package org.apache.axis2.mgmt.api;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mukund
 * Date: Mar 3, 2006
 * Time: 4:03:21 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Aggregator {

    public long getRequestCount();
    public long getRequestCount(String filter);

    public long getFaultCount();
    public long getFaultCount(String filter);

    public double getAverageResponseTime();
    public double getAverageResponseTime(String filter);

    public long getLastResponseTime();
    public long getLastResponseTime(String filter);

    public long getWindow();
    public void setWindow(long val);

    public void setWindowMode(int mode); // TIME, REQUESTS
    public int getWindowMode();

    public void setFilters(List filters);
    public List getFilters();

}
