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

package org.apache.sandesha2;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;

/**
 * Used to hold data related to a RM Fault.
 * 
 */

public class FaultData {

	private int type;

	private String code;

	private QName subcode;

	private String reason;

	private OMElement detail;
	
	private OMElement detail2;

	private String detailString;

	private String sequenceId;
	
	private String exceptionString;


	public OMElement getDetail() {
		return detail;
	}

	public void setDetail(OMElement detail) {
		this.detail = detail;
	}

	public void setDetail2(OMElement detail2) {
		this.detail2 = detail2;
  }

	public OMElement getDetail2() {
		return detail2;
	}
	
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public QName getSubcode() {
		return subcode;
	}

	public void setSubcode(QName subcode) {
		this.subcode = subcode;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(String sequenceId) {
		this.sequenceId = sequenceId;
	}

	public String getDetailString() {
  	return detailString;
  }

	public void setDetailString(String detailString) {
  	this.detailString = detailString;
  }

	public String getExceptionString() {
  	return exceptionString;
  }

	public void setExceptionString(String exceptionString) {
  	this.exceptionString = exceptionString;
  }

}
