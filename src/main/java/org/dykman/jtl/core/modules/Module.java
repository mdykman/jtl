package org.dykman.jtl.core.modules;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.future.AsyncExecutionContext;

public interface Module {
//	void config(JSONObject config);
	void define(AsyncExecutionContext<JSON> parent);
}
