package org.dykman.jtl.modules;

import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

public interface Module {
//	void config(JSONObject config);
	void define(SourceInfo meta,AsyncExecutionContext<JSON> parent);
}
