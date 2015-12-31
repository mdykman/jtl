package org.dykman.jtl.modules;

import org.dykman.jtl.json.JSONObject;

public abstract class AbstractModule implements Module {
	
	final JSONObject config;	

	final String bindingKey;
	
	public AbstractModule (String key,JSONObject config) {
		bindingKey = key;
		this.config = config;
	}
	
}
