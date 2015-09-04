package org.dykman.jtl.future;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DeferredCall implements InstructionFuture<JSON> {
   final SourceInfo info;
	public final InstructionFuture<JSON> inst;
	public AsyncExecutionContext<JSON> pcontext;
	public final ListenableFuture<JSON> data;

	public DeferredCall(SourceInfo source,InstructionFuture<JSON> inst,
			AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> t) {
	   this.inst = inst;
		this.pcontext = context;
		this.data = t;
		this.info = source;
	}



	@Override
	public InstructionFuture<JSON> getBareInstruction() {
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
	     return new DeferredCall(info, inst, context, data);
	  }
	@Override
	public InstructionFuture<JSON> unwrap() {
      return new DeferredCall(info,inst.unwrap(), pcontext, null);
	}

	@Override
	public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException {
	   ListenableFuture<JSON> d = this.data != null ? this.data : data;
		return inst.call(this.pcontext, d);
	}
}
