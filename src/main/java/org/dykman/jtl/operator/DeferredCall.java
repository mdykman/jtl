package org.dykman.jtl.operator;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class DeferredCall implements FutureInstruction<JSON> {
   final SourceInfo info;
	public final FutureInstruction<JSON> inst;
	public AsyncExecutionContext<JSON> pcontext;
	public final ListenableFuture<JSON> data;

	public DeferredCall(FutureInstruction<JSON> inst,
			AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> t) {
	   this.inst = inst;
		this.pcontext = context;
		this.data = t;
		this.info = inst.getSourceInfo();
	}



	@Override
	public FutureInstruction<JSON> getBareInstruction() {
	   return inst.getBareInstruction();
	}

	public SourceInfo getSourceInfo() {
	   return info;
	}
//   @Override
	public AsyncExecutionContext<JSON> getContext() {
	   return pcontext;
	}
	
	  public DeferredCall rebindContext(final AsyncExecutionContext<JSON> context) {
	     return new DeferredCall(inst, context, data);
	  }
	@Override
	public FutureInstruction<JSON> unwrap() {
      return new DeferredCall(inst.unwrap(), pcontext, null);
	}

	@Override
	public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException {
	   ListenableFuture<JSON> d = this.data != null ? this.data : data;
		return inst.call(this.pcontext, d);
	}
}
