package org.dykman.jtl.future;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilderImpl;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.transform;
import static org.dykman.jtl.future.FutureInstructionFactory.*;

public abstract class ObjectInstructionBase extends AbstractFutureInstruction {
	final List<Pair<ObjectKey, FutureInstruction<JSON>>> ll;

	AsyncExecutionContext<JSON> initContext = null;
	FutureInstruction<JSON> initInst = null;
	ListenableFuture<JSON> initResult = null;
	boolean isContextObject = false;

	static  class ObjectKey {
		final String label;
		final boolean quoted;
		public ObjectKey(String l, boolean b) {
			label = l;
			quoted = b;
		}
	}

	public ObjectInstructionBase(SourceInfo info, List<Pair<ObjectKey, FutureInstruction<JSON>>> pp, boolean itemize) {
		super(info, true);
		ll = pp;
	}

	public abstract ListenableFuture<JSON> _callObject(
			final List<Pair<ObjectKey, FutureInstruction<JSON>>> fields, 
			final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data) throws ExecutionException;

	public final ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data) throws ExecutionException {

		FutureInstruction<JSON> init = null;
		List<Pair<ObjectKey, FutureInstruction<JSON>>> fields = new ArrayList<>();
		List<FutureInstruction<JSON>> imperitives = new ArrayList<>(ll.size());

		for (Pair<ObjectKey, FutureInstruction<JSON>> ii : ll) {
			String k = ii.f.label;
			char ic = k.charAt(0);
			boolean notquoted = !ii.f.quoted;
			if (notquoted && "!init".equals(k)) {
				init = ii.s;
			} else if (notquoted && ic == '$') {
				FutureInstruction<JSON> imp = fixContextData(
						FutureInstructionFactory.deferred(ii.s.getSourceInfo(), ii.s, context, data));
				context.define(k.substring(1), imp);
			} else if (notquoted && ic == '!') {
				FutureInstruction<JSON> imp = fixContextData(ii.s);
				context.define(k.substring(1), imp);
				imperitives.add(imp);
			} else {
				fields.add(ii);
			}
		}
		if(init != null || imperitives.size() > 0 || (isContextObject && (fields.size() > 0))) {
			context = context.createChild(false, false, data, source);
		}
		final      AsyncExecutionContext<JSON> ctx = context;
		return transform(runImperatives(init, imperitives, context, data), new AsyncFunction<JSON, JSON>() {
			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {
				return _callObject(fields, ctx, data);
			}
		});
		// }

	}

	public static  ListenableFuture<JSON> runImperatives(final FutureInstruction<JSON> init,
			final List<FutureInstruction<JSON>> imperitives, final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data) throws ExecutionException {
		try {
			// ensure that init is completed so that any modules are
			// installed
			// and imports imported
			// final FutureInstruction<JSON> finst = init;
			AsyncFunction<JSON, JSON> runner = new AsyncFunction<JSON, JSON>() {
				@Override
				public ListenableFuture<JSON> apply(final JSON input) throws Exception {
					List<ListenableFuture<JSON>> ll = new ArrayList<>();
					for (FutureInstruction<JSON> imp : imperitives) {
						ll.add(imp.call(context, data));
					}
					return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

						@Override
						public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
							// TODO overlay the series of init results into a single one
							return immediateCheckedFuture(JSONBuilderImpl.NULL);
						}
					});
				}
			};

			if (init != null)
				return transform(init.call(context, data), runner);
			return runner.apply(JSONBuilderImpl.NULL);
		} catch (Exception e) {
			FutureInstruction<JSON> error = context.getdef("error");
			if (error == null) {
				logger.error("WTF!!!!???? no error handler is defined!");
				throw new RuntimeException("WTF!!!!???? no error handler is defined!");
			}
			SourceInfo info = SourceInfo.internal("imperatives");
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
		ctx.inject(initContext);
		for (Map.Entry<String, AsyncExecutionContext<JSON>> nc : initContext.getNamedContexts().entrySet()) {
			ctx.inject(nc.getKey(), nc.getValue());
		}
		return initResult;
	}

	public List<Pair<ObjectKey, FutureInstruction<JSON>>> pairs() {
		return ll;
	};
}