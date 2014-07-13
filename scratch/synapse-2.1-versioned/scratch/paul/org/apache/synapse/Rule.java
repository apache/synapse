package org.apache.synapse;

public class Rule {
	private String xpath = null;
	private String mediatorName = null;
	private boolean secure = false;
	private boolean reliable = false;
	private boolean transactional = false;
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
	public String getXpath() {
		return xpath;
	}
	public void setMediatorName(String mediatorName) {
		this.mediatorName = mediatorName;
	}
	public  String getMediatorName() {
		return mediatorName;
	}
	public void setSecure(boolean secure) {
		this.secure = secure;
	}
	public  boolean isSecure() {
		return secure;
	}
	public  void setReliable(boolean reliable) {
		this.reliable = reliable;
	}
	public boolean isReliable() {
		return reliable;
	}
	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}
	public boolean isTransactional() {
		return transactional;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		sb.append('\'');
		sb.append(xpath);
		sb.append('\'');
		sb.append('}');
		sb.append(' ');
		sb.append('-');
		sb.append('>');
		sb.append(' ');
		sb.append(mediatorName);
		if (isReliable()) { sb.append(','); sb.append("reliable"); }
		if (isSecure()) { sb.append(','); sb.append("secure"); }
		if (isTransactional()) { sb.append(','); sb.append("transactional"); }
		sb.append(';');
		
		return sb.toString();
	}
}
