package org.apache.axis2.mgmt.model;

import org.apache.axis2.om.OMElement;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mukund
 * Date: Mar 3, 2006
 * Time: 4:02:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransactionEvent {
    boolean success;
    long rtt;
    String operation;
    Request request;
    Response response;
}

class Request {
    String transport;
    List headers;
    long timestamp;
    OMElement content;
}

class Response {
    long timestamp;
    List headers;
    OMElement content;
}

class Header {
    String name, value;
}