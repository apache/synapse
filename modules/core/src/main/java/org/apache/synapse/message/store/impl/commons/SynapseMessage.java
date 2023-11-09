package org.apache.synapse.message.store.impl.commons;

import org.apache.synapse.SynapseConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class serves as a container for the Synapse Message Context parameters/properties
 * , and it will be saved as a message in the Store.
 */
public class SynapseMessage implements Serializable {
    private ArrayList<String> localEntries = new ArrayList<String>();

    private HashMap<String, String> properties = new HashMap<String, String>();

    private HashMap<String, byte[]> propertyObjects = new HashMap<String, byte[]>();

    private boolean response = false;

    private boolean faultResponse = false;

    private int tracingState = SynapseConstants.TRACING_UNSET;

    public boolean isResponse() {
        return response;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }

    public boolean isFaultResponse() {
        return faultResponse;
    }

    public void setFaultResponse(boolean faultResponse) {
        this.faultResponse = faultResponse;
    }

    public int getTracingState() {
        return tracingState;
    }

    public void setTracingState(int tracingState) {
        this.tracingState = tracingState;
    }

    public List<String> getLocalEntries() {
        return localEntries;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public HashMap<String, byte[]> getPropertyObjects() {
        return propertyObjects;
    }

    public void addProperty(String key,String value) {
        properties.put(key,value);
    }

    public void addPropertyObject(String key , byte[] value){
        propertyObjects.put(key, value);
    }

    public void addLocalEntry(String key) {
        localEntries.add(key);
    }
}
