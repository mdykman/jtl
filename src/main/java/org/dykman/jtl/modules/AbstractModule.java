package org.dykman.jtl.modules;

public abstract class AbstractModule implements Module {
	
	
	String bindingKey;
	
	public void setKey(String key) {
		this.bindingKey = key;
	}

}
