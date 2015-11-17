package org.dykman.jtl.future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class FunctionInvocationInstruction extends AbstractFutureInstruction {
	final String name;
	final List<FutureInstruction<JSON>> iargs;
	boolean variable = false;

	final FutureInstruction<JSON> instruction;
	public FunctionInvocationInstruction(SourceInfo info, String name, List<FutureInstruction<JSON>> iargs) {
		this(info,name,iargs,null);
	}
	public FunctionInvocationInstruction(SourceInfo info, String name, List<FutureInstruction<JSON>> iargs,
			FutureInstruction<JSON> instruction) {
		super(info);
		this.name = name;
		this.iargs = iargs;
		this.instruction = instruction;
	}

	public void setVariable(boolean b) {
		variable = b;
	}

	protected AsyncExecutionContext<JSON> setupArguments(final AsyncExecutionContext<JSON> dctx,
			final AsyncExecutionContext<JSON> fc, final String name, final List<FutureInstruction<JSON>> iargs,
			final ListenableFuture<JSON> data) {
		AsyncExecutionContext<JSON> context = dctx.createChild(true, false, data, source);
		List<FutureInstruction<JSON>> insts = new ArrayList<>();
		if(name.contains(".")) {
			String pp[] = name.split("[.]");
			context.define("0", FutureInstructionFactory.value(context.builder().value(pp[1]), source));
		} else {
			context.define("0", FutureInstructionFactory.value(context.builder().value(name), source));
		}
//		context.define("0", InstructionFutureFactory.value(context.builder().value(name), source));
		int cc = 1;
		// Iterator<InstructionFuture<JSON>> iit = iargs.iterator();
		if (iargs != null)
			for (FutureInstruction<JSON> inst : iargs) {
				String key = Integer.toString(cc++);
				context.define(key, FutureInstructionFactory.memo(source,
						FutureInstructionFactory.deferred(source, inst, dctx.declaringContext(), data)));
				insts.add(inst);
			}

 		// dctx.isFunctionContext();
		if ((!variable) && dctx.declaringContext()!= dctx && fc != null) {
			int ss = 1;
			while (true) {
				String skey = Integer.toString(ss++);
				FutureInstruction<JSON> si = dctx.getdef(skey);
				if (si == null)
					break;
				String key = Integer.toString(cc++);
				// context.define(key, new DeferredCall(source, si, ctx,
				// data));
				context.define(key, si);
				insts.add(si);
			}

		}
		context.define("@", FutureInstructionFactory.paramArray(source, insts));

		return context;
	}

	public static boolean isSpecial(String s) {
		return SimpleExecutionContext.isSpecial(s);
	}

	@Override

	public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> data)
			throws ExecutionException {
		final AsyncExecutionContext<JSON> fc = context.getFunctionContext();
		String ns = null;
		AsyncExecutionContext<JSON> childContext = setupArguments(context, fc, name, iargs, data);
		FutureInstruction<JSON> func = instruction;
		if(func == null) {
			Pair<String, FutureInstruction<JSON>> rr = context.getDefPair(name);
			if (rr != null) {
				func = rr.s;
				ns = rr.f;
				childContext.define("$", FutureInstructionFactory.value(
						context.builder().value(ns == null ? "" : ns), source));
			}
		}

		if (func == null) {
			throw new ExecutionException("no function found named " + name, source);
		}
		// we still want to preserve the context namespace even when executing unnamed functions
		if (ns != null) {
			childContext.setNamespace(ns);
		}

		return func.call(childContext, data);
	}
}