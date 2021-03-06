package org.dykman.jtl.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilderImpl;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.transform;
import static org.dykman.jtl.operator.FutureInstructionFactory.*;

public abstract class ObjectInstructionBase extends AbstractFutureInstruction {
	final List<Pair<ObjectKey, FutureInstruction<JSON>>> ll;

	AsyncExecutionContext<JSON> initContext = null;
	FutureInstruction<JSON> initInst = null;
	ListenableFuture<JSON> initResult = null;
	boolean isContextObject = false;

	public static class ObjectKey {
		public final String label;
		public final boolean quoted;
		public final FutureInstruction<JSON> expr;

		public ObjectKey(FutureInstruction<JSON> expr) {
			this.expr = expr;
			this.label = null;
			this.quoted = false;
		}
		public ObjectKey(String l, boolean b) {
			label = l;
			quoted = b;
			expr = null;
		}
	}

	public ObjectInstructionBase(SourceInfo info, List<Pair<ObjectKey, FutureInstruction<JSON>>> pp, boolean itemize) {
		super(info, itemize);
		ll = pp;
	}

	public abstract ListenableFuture<JSON> _callObject(
			final List<Pair<ObjectKey, FutureInstruction<JSON>>> fields, 
			final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data,
			List<Pair<ObjectKey, FutureInstruction<JSON>>> deferred) throws ExecutionException;

	public final ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> data)
			throws ExecutionException {

		FutureInstruction<JSON> init = null;
		int ss = ll.size();
		List<Pair<ObjectKey, FutureInstruction<JSON>>> fields = new ArrayList<>(ss);
		List<FutureInstruction<JSON>> imperitives = new ArrayList<>(ss);
		List<Pair<ObjectKey, FutureInstruction<JSON>>> variables = new ArrayList<>(ss);
		List<Pair<ObjectKey, FutureInstruction<JSON>>> functions = new ArrayList<>(ss);
		final List<Pair<ObjectKey, FutureInstruction<JSON>>> deferred = new ArrayList<>(ss);
		AsyncExecutionContext<JSON> newContext = context.createChild(false, false, data, source);
		for (Pair<ObjectKey, FutureInstruction<JSON>> ii : ll) {
			String k = ii.f.label;
			if(k == null && ii.f.expr != null) {
				deferred.add(ii);
				continue;
			}
			char ic = k.charAt(0);
			boolean notquoted = !ii.f.quoted;
			if (notquoted && (ic == '!' || ic == '$')) {
				FutureInstruction<JSON> fi = null;
				if (k.length() < 2)
					throw new ExecutionException("unnamed variable: " + k, source);
				if("!init".equals(ii.f.label) && ii.f.quoted) {
					fi = init = memo(deferred(ii.s, newContext, context.config()));
				} else {
					fi = memo(deferred( ii.s, newContext, data));
					if(ic == '!') {
						imperitives.add(fi);
					}
				}
				variables.add(new Pair<>(ii.f,fi));
			} else if ((!isContextObject) || (isContextObject && notquoted && "_".equals(k))) {
				fields.add(ii);
			} else {
				functions.add(new Pair<ObjectInstructionBase.ObjectKey, FutureInstruction<JSON>>(
						new ObjectKey(k, false), fixContextData(ii.s,newContext)));
			}
		}
		
		if (variables.size() > 0 || functions.size() > 0) {
			context = newContext;
		}

		for (Pair<ObjectKey, FutureInstruction<JSON>> ii : variables) {
			boolean notquoted = !ii.f.quoted;
			String k = ii.f.label;
			char ic = k.charAt(0);
			
			if (notquoted && (ic == '!' || ic == '$')) {
				k = k.substring(1);
				FutureInstruction<JSON> imp = memo(ii.s);
				context.define(k, imp);
			} else {
				throw new ExecutionException("unexpected variable " + k,source);
			}
		}
		
		for (Pair<ObjectKey, FutureInstruction<JSON>> ii : functions) {
			context.define(ii.f.label,fixContextData(ii.s,context));
		}
		
		final AsyncExecutionContext<JSON> ctx = context;
		return transform(runImperatives(init, imperitives, ctx, data), new AsyncFunction<JSON, JSON>() {
			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {

				// init and imperatives ignored by dataobject
				//List<ListenableFuture<JSON>> list = new ArrayList<>();
				
				return _callObject(fields, ctx, data, deferred);
			}
		});

	}

	public static ListenableFuture<JSON> runImperatives(final FutureInstruction<JSON> init,
			final List<FutureInstruction<JSON>> imperitives, final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data) throws ExecutionException {
		try {
			AsyncFunction<JSON, JSON> runner = new AsyncFunction<JSON, JSON>() {
				@Override
				public ListenableFuture<JSON> apply(final JSON input) throws Exception {
					// input is the result of the init instruction, if any
					// and is not directly relevant to the other imperatives
					// as long as it is complete
					List<ListenableFuture<JSON>> ll = new ArrayList<>();
					for (FutureInstruction<JSON> imp : imperitives) {
						// imperatives, who underlaying function has been
						// memo0zed higher
						// up, can be set in motion without directly caring
						// about the outcome.
						// they will leave a variable in the context for which
						// they were declared
						// in case anyone is interested in the outcome
						ll.add(imp.call(context, data));
					}
					JSONArray arr = context.builder().array(null);
					// force synchronous resolution for imperatives
					for (JSON j : allAsList(ll).get()) {
						arr.add(j);
					}
					return immediateCheckedFuture(arr);
				}
			};

			if (init != null)
				return transform(init.call(context, context.config()), runner);
			return runner.apply(JSONBuilderImpl.NULL);
		} catch (Exception e) {
			FutureInstruction<JSON> error = context.getdef("error");
			if (error == null) {
				logger.error("WTF!!!!???? no error handler is defined!");
				throw new RuntimeException("WTF!!!!???? no error handler is defined!", e);
			}
			SourceInfo info = e instanceof ExecutionException ? ((ExecutionException) e).getSourceInfo()
					: SourceInfo.internal("error");

			AsyncExecutionContext<JSON> ec = context.createChild(true, false, data, info);
			ec.define("0", FutureInstructionFactory.value("error", context.builder(), info));
			ec.define("1", FutureInstructionFactory.value(500L, context.builder(), info));
			ec.define("2", FutureInstructionFactory.value(e.getLocalizedMessage(), context.builder(), info));
			return error.call(ec, data);
		}
	}

	protected ListenableFuture<JSON> initializeContext(AsyncExecutionContext<JSON> ctx, FutureInstruction<JSON> inst,
			ListenableFuture<JSON> data) throws ExecutionException {
		if (initContext == null) {
			synchronized (this) {
				if (initContext == null) {
					initResult = inst.call(ctx, data);
					initContext = ctx;
				}
			}
		}
		// TODO:: I think this 'injection' cycle is entirely unneccessry
		/*
		 * ctx.inject(initContext); for (Map.Entry<String,
		 * AsyncExecutionContext<JSON>> nc :
		 * initContext.getNamedContexts().entrySet()) { ctx.inject(nc.getKey(),
		 * nc.getValue()); }
		 */
		return initResult;
	}

	public List<Pair<ObjectKey, FutureInstruction<JSON>>> pairs() {
		return ll;
	};
}
