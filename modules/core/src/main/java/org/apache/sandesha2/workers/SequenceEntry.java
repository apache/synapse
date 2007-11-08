/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.workers;

import java.io.Serializable;

public class SequenceEntry implements Serializable {
	private static final long serialVersionUID = -6823171634616402792L;

	private String  sequenceId;
	private boolean rmSource;
	
	public SequenceEntry(String sequenceId, boolean rmSource) {
		this.sequenceId = sequenceId;
		this.rmSource = rmSource;
	}
	public boolean isRmSource() {
		return rmSource;
	}
	public String getSequenceId() {
		return sequenceId;
	}


	public boolean equals(Object o) {
		if(o == null) return false;
		if(o == this) return true;
		if(o.getClass() != getClass()) return false;
		
		SequenceEntry other = (SequenceEntry) o;
		if(sequenceId != null) {
			if(!sequenceId.equals(other.sequenceId)) return false;
		} else {
			if(other.sequenceId != null) return false;
		}
		
		return rmSource == other.rmSource;
	}
	public int hashCode() {
		int result = 1;
		if(sequenceId != null) result = sequenceId.hashCode();
		if(rmSource) result = -result;
		return result;
	}
}
