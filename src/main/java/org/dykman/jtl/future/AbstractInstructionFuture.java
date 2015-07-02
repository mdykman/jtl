package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractInstructionFuture implements
		InstructionFuture<JSON> {

	@Override
	public InstructionFuture<JSON> unwrap(AsyncExecutionContext<JSON> context) {
		return this;
	}

	protected String stringValue(JSON j) {
		if (j == null)
			return null;
		switch (j.getType()) {
		case STRING:
		case DOUBLE:
		case LONG:
			return ((JSONValue) j).stringValue();
		default:
			return null;
		}
	}

	public abstract ListenableFuture<JSON> call(
			AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException;

}
