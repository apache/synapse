package com.infravio.core;

import java.util.Enumeration;
import java.util.Vector;

public class SynapseObjectArray extends Vector {
    public SynapseObjectArray() {
    }

    public void addSynapseObject(SynapseObject bo) {
        this.add(bo);
    }

    public void addSynapseObjects(SynapseObject[] bos) {
        int length = bos.length;
        int i;
        for (i = 0; i < length; i++) {
            this.add(bos[i]);
        }
    }

    public SynapseObject[] getSynapseObjects() {
        int count = this.size();
        int index = 0;
        SynapseObject[] children = new SynapseObject[count];
        Enumeration enumeration = this.elements();
        while (enumeration.hasMoreElements()) {
            children[index++] = (SynapseObject)enumeration.nextElement();
        }
        return children;
    }
}