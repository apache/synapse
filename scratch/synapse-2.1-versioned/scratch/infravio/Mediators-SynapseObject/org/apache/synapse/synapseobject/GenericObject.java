package org.apache.synapse.synapseobject;

import java.io.Serializable;

/**
 * This class holds the actual values of a name-value pair with type, this will be 
 * internally used by SynapseObject to store attributes
 */
public class GenericObject implements Serializable {
    String name;
    String type;
    String value;
/**
   * 
   * Default constructor which forces the value to be populated during instantiation
   */
    public GenericObject(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

     public String getValue() {
        return value;
    }
}