package com.infravio.core;

import java.io.Serializable;

/**
 * @author
 */
public class GenericObject implements Serializable {
    String name;
    String type;
    String value;

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