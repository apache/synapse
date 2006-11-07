/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.apache.sandesha2.storage.beans;

/**
 * This bean is used to store properties of a certain sequence.
 * Used by both sending and receiving sides.
 */

public class SequencePropertyBean extends RMBean {

	private static final long serialVersionUID = 8266532177909565832L;

	/**
	 * Comment for <code>sequencePropertyKey</code>
	 * The key used to store properties of this sequence.
	 * The value depends on the endpoint as given below.
	 * 
	 * RMS (sending side) - internalSequenceId
	 * RMD - sequenceId
	 */
	private String sequencePropertyKey;

	/**
	 * Comment for <code>name</code>
	 * The name of the property. Possible names are given in the Sandesha2Constants.SequenceProperties interface.
	 */
	private String name;

	/**
	 * Comment for <code>value</code>
	 * The value of the property.
	 */
	private String value;

	public SequencePropertyBean(String seqID, String propertyName, String value) {
		this.sequencePropertyKey = seqID;
		this.name = propertyName;
		this.value = value;
	}

	public SequencePropertyBean() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSequencePropertyKey() {
		return sequencePropertyKey;
	}

	public void setSequencePropertyKey(String sequencePropertyKey) {
		this.sequencePropertyKey = sequencePropertyKey;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}