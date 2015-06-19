package org.dykman.jtl.core;

import org.dykman.jtl.core.engine.future.AsyncExecutionContext;

public interface Module {
//	void config(JSONObject config);
	void define(AsyncExecutionContext<JSON> parent, JSONBuilder builder,JSONObject config);
}
