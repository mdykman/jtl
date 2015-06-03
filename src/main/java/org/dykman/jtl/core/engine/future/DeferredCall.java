package org.dykman.jtl.core.engine.future;

import  org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

public class DeferredCall implements InstructionFuture<JSON>{
	final InstructionFuture<JSON> inst; 
	final AsyncExecutionContext<JSON> context;
	final ListenableFuture<JSON> t;
	
	public DeferredCall(
			final InstructionFuture<JSON> inst, 
			final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> t) {
		this.inst = inst;
		this.context = context;
		this.t = t;
	}

	@Override
	public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException {
		return inst.call(this.context, this.t);
	}
}
