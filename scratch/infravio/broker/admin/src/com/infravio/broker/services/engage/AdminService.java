package com.infravio.broker.services.engage;

import org.apache.axis2.om.OMElement;

import javax.xml.stream.XMLStreamException;

public class AdminService {

    public void update(OMElement omelement) throws XMLStreamException {

        System.out.println("Admin Service Hit!");

    }

}