package org.dykman.jtl.future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class FunctionInstruction extends AbstractInstructionFuture {
	final String name;
	final List<InstructionFuture<JSON>> iargs;
	boolean variable = false;

	public FunctionInstruction(SourceInfo info, String name, List<InstructionFuture<JSON>> iargs) {
		super(info);
		this.name = name;
		this.iargs = iargs;
	}

	public void setVariable(boolean b) {
		variable = b;
	}

	protected AsyncExecutionContext<JSON> setupArguments(final AsyncExecutionContext<JSON> dctx,
			final AsyncExecutionContext<JSON> fc, final String name, final List<InstructionFuture<JSON>> iargs,
			final ListenableFuture<JSON> data) {
		AsyncExecutionContext<JSON> context = dctx.createChild(true, false, data, source);
		List<InstructionFuture<JSON>> insts = new ArrayList<>();
		if(name.contains(".")) {
			String pp[] = name.split("[.]");
			context.define("0", InstructionFutureFactory.value(context.builder().value(pp[1]), source));
		} else {
			context.define("0", InstructionFutureFactory.value(context.builder().value(name), source));
		}
//		context.define("0", InstructionFutureFactory.value(context.builder().value(name), source));
		int cc = 1;
		// Iterator<InstructionFuture<JSON>> iit = iargs.iterator();
		if (iargs != null)
			for (InstructionFuture<JSON> inst : iargs) {
				String key = Integer.toString(cc++);
				context.define(key, InstructionFutureFactory.memo(source,
						InstructionFutureFactory.deferred(source, inst, dctx.declaringContext(), data)));
				insts.add(inst);
			}

		// AsyncExecutionContext<JSON> declaring = dctx.declaringContext();
		// boolean subst = (!variable); // && (dctx != declaring) &&
		// dctx.isFunctionContext();
		if ((!variable) && dctx.declaringContext()!= dctx && fc != null) {
			int ss = 1;
			while (true) {
				String skey = Integer.toString(ss++);
				InstructionFuture<JSON> si = dctx.getdef(skey);
				if (si == null)
					break;
				String key = Integer.toString(cc++);
				// context.define(key, new DeferredCall(source, si, ctx,
				// data));
				context.define(key, si);
				insts.add(si);
			}

		}
		context.define("@", InstructionFutureFactory.paramArray(source, insts));

		return context;
	}

	// ArrayList<String> codePath = new ArrayList<>();
	/*
	 * InstructionFuture<JSON> resolve(final AsyncExecutionContext<JSON>
	 * context, String ns, String name) { Set<String> tried = new HashSet<>();
	 * InstructionFuture<JSON> r = null; if (ns == null) { int ptr =
	 * codePath.size() - 1; while (ptr >= 0) { String s = codePath.get(ptr); if
	 * (!tried.contains(s)) { tried.add(s); r = context.getdef(s, name); if (r
	 * != null) return r; } --ptr; } } }
	 */
	static boolean isSpecial(String s) {
		if (SpecialSymbols.contains(s))
			return true;
		if (Character.isDigit(s.charAt(0)))
			return true;
		return false;

	}

	static List SpecialSymbols;

	static {
		SpecialSymbols = Arrays.asList(new String[] { "@", "?", ":", ";", "#", "!", "%", "^", "&", "*" });
	}

	@Override

	public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> data)
			throws ExecutionException {
		final AsyncExecutionContext<JSON> fc = context.getFunctionContext();
		String ns = null;
		if (fc != null && !isSpecial(name)) {
			ns = fc.getNamespace();
		}
		AsyncExecutionContext<JSON> childContext = setupArguments(context, fc, name, iargs, data);
		InstructionFuture<JSON> func = null;
		Pair<String, InstructionFuture<JSON>> rr = context.getDefInternal(ns, name);
		if (rr != null) {
			func = rr.s;
			ns = rr.f;
			childContext.define("$", InstructionFutureFactory.value(context.builder().value(ns == null ? "" : ns), source));
		}

		if (func == null) {
			throw new ExecutionException("no function found named " + name, source);
		}
		if (ns != null) {
			childContext.setNamespace(ns);
		}

		return func.call(childContext, data);
	}
}