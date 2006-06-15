package org.apache.sandesha2.transport;

import javax.xml.namespace.QName;

import org.apache.axis2.description.TransportOutDescription;

public class Sandesha2TransportOutDesc extends TransportOutDescription {

    public Sandesha2TransportOutDesc() {
        super (new QName ("Sandesha2TransportOutDesc"));
        this.setSender(new Sandesha2TransportSender ());
    }
    
    
	
}
