package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class FixedContext extends AbstractInstructionFuture {
	final InstructionFuture<JSON> inst;

	public FixedContext(final InstructionFuture<JSON> inst) {
		super(inst.getSourceInfo());
		this.inst = inst;
	};
	public InstructionFuture<JSON> getIntruction() {
		return inst;
	}
	@Override
	public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException {
		context.define("_", InstructionFutureFactory.value(data, inst.getSourceInfo()));
		context.declaringContext(context);
		return inst.call(context, data);
	}
}