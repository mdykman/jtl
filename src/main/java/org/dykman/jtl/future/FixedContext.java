package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.operator.AbstractFutureInstruction;
import org.dykman.jtl.operator.FutureInstruction;
import org.dykman.jtl.operator.FutureInstructionFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class FixedContext extends AbstractFutureInstruction {
	final FutureInstruction<JSON> inst;

	public FixedContext(final FutureInstruction<JSON> inst) {
		super(inst.getSourceInfo());
		this.inst = inst;
	};
	public FutureInstruction<JSON> getIntruction() {
		return inst;
	}
	@Override
	public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException {
		context.define("_", FutureInstructionFactory.value(data, inst.getSourceInfo()));
		context.declaringContext(context);
		return inst.call(context, data);
	}
}