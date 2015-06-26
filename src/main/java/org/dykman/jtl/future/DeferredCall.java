package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONException;

import com.google.common.util.concurrent.ListenableFuture;

public class DeferredCall implements InstructionFuture<JSON>{
	public final InstructionFuture<JSON> inst; 
	public final AsyncExecutionContext<JSON> context;
	public final ListenableFuture<JSON> t;
	
	public DeferredCall(
			final InstructionFuture<JSON> inst, 
			final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> t) {
		this.inst = inst;
		this.context = context;
		this.t = t;
	}
	
	@Override
	public InstructionFuture<JSON> unwrap() {
		return inst.unwrap();
	}

	@Override
	public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException {
		return inst.call(this.context, this.t);
	}
}
