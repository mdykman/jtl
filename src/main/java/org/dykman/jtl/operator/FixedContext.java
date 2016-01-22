package org.dykman.jtl.operator;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class FixedContext extends AbstractFutureInstruction {
	final FutureInstruction<JSON> inst;
	final AsyncExecutionContext<JSON> context;
	public FixedContext(final FutureInstruction<JSON> inst) {
		this(inst,null);
	}
	
	public FixedContext(final FutureInstruction<JSON> inst,AsyncExecutionContext<JSON> context) {
		super(inst.getSourceInfo());
		this.inst = inst;
		this.context = context;
	};
	public FutureInstruction<JSON> getIntruction() {
		return inst;
	}
	@Override
	public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException {
		AsyncExecutionContext<JSON> ctx = this.context !=null ? this.context : context;
//		ctx=ctx.createChild(true, false, data, source);
//		ctx.define("_", FutureInstructionFactory.value(data, inst.getSourceInfo()));
		// ??
		ctx.declaringContext(ctx);
		return inst.call(context, data);
	}
}