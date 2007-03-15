/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.bsf.convertors;

import org.apache.axiom.om.OMElement;
import org.apache.bsf.BSFEngine;

/**
 * The OMElementConvertor interface enables customizing the conversion of 
 * XML between Synapse and a script language. Some script languages have their
 * own ways of using XML, such as E4X in JavaScript or REXML in Ruby. But BSF
 * has no support for those so Synapse needs to handle this itself, which is what
 * the OMElementConvertor does.
 * 
 * Which OMElementConvertor type to use is determined by the script language specified for
 * the mediator script. If a suitable convertor class is not found then a default convertor
 * is used which converts XML to a String representation.
 */
public interface OMElementConvertor {

    /** Set a reference to the BSFEngine to evalue the script */
    public void setEngine(BSFEngine e);

    /** Convert the OMElement to a suitable script object for the scripting language */
    public Object toScript(OMElement omElement);

    /** Convert a scripting language object into an OMElement */
    public OMElement fromScript(Object o);
}
