package org.dykman.jtl.core.engine.future;

import org.dykman.jtl.core.JSON;

import com.google.common.util.concurrent.ListenableFuture;

class DeferredCall {
	final InstructionFuture<JSON> inst; 
	final AsyncExecutionContext<JSON> context;
	final ListenableFuture<JSON> t;
	
	public DeferredCall(final InstructionFuture<JSON> inst, 
	final AsyncExecutionContext<JSON> context,
	final ListenableFuture<JSON> t) {
		this.inst = inst;
		this.context = context;
		this.t = t;
	}
}