package org.dykman.jtl.modules;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

public interface Module {
	JSON define(SourceInfo meta,AsyncExecutionContext<JSON> parent, boolean serverMode)
		throws ExecutionException;
}
