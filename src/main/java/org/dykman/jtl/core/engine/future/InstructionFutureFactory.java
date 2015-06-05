package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateFailedCheckedFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.Pair;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class InstructionFutureFactory {

	static InstructionFuture<JSON> NULLINST = null;
	static InstructionFuture<JSON> TRUEINST = null;
	static InstructionFuture<JSON> FALSEINST = null;

	final JSONBuilder builder;

	public InstructionFutureFactory(JSONBuilder builder) {
		this.builder = builder;
	}

	public static InstructionFuture<JSON> memo(
			final InstructionFuture<JSON> inst) {
		return new AbstractInstructionFuture<JSON>() {
			private ListenableFuture<JSON> result = null;
			private boolean fired = false;

			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				if (!fired) {
					synchronized (this) {
						if (!fired) {
							result = inst.call(context, data);
							fired = true;
						}
					}
				}
				return result;
			}
		};
	}

	public InstructionFuture<JSON> variable(final String name) {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) {
				try {
					if (t == null)
						return immediateFuture(builder.value());
					return context.lookup(name, t);
				} catch (Exception e) {
					return immediateFailedCheckedFuture(new ExecutionException(
							e));
				}
			}
		};
	}

	public InstructionFuture<JSON> deferred(InstructionFuture<JSON> inst,
			AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t) {
		return memo(new DeferredCall(inst, context, t));
	}

	public InstructionFuture<JSON> function(final String name,
			final List<InstructionFuture<JSON>> iargs) {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) throws ExecutionException {
				InstructionFuture<JSON> func = context.getdef(name);
				if (func == null) {
					System.err.println("function '" + name + "' not found.");
					return immediateFuture(null);
				}
				// a function context has numeric-labeled functions in its table
				// which should not fall back to the parent
				AsyncExecutionContext<JSON> childContext = context
						.createChild(true);

				List<InstructionFuture<JSON>> a = new ArrayList<>(iargs.size());
				int cc = 0;
				for (InstructionFuture<JSON> i : iargs) {
					InstructionFuture<JSON> inst = deferred(i, context, t);
					childContext.define(Integer.toString(cc++), inst);
					a.add(inst);
				}
				return func.call(childContext, t);
			}
		};
	}

	public static InstructionFuture<JSON> value(final ListenableFuture<JSON> o) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return o;
			}
		};
	}

	public static InstructionFuture<JSON> value(JSON o) {
		return value(immediateFuture(o));
	}

	public InstructionFuture<JSON> value() {
		return value(builder.value());
	}

	public InstructionFuture<JSON> value(final Boolean val) {
		return value(builder.value(val));
	}

	public InstructionFuture<JSON> value(final Long val) {
		return value(builder.value(val));
	}

	public InstructionFuture<JSON> value(final Double val) {
		return value(builder.value(val));
	}

	public InstructionFuture<JSON> value(final String val) {
		return value(builder.value(val));
	}

	public InstructionFuture<JSON> number(TerminalNode i, TerminalNode d) {
		return value(i != null ? Long.parseLong(i.getText()) : Double
				.parseDouble(d.getText()));
	}

	public InstructionFuture<JSON> array(final List<InstructionFuture<JSON>> ch) {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) throws ExecutionException {
				List<ListenableFuture<JSON>> args = new ArrayList<>();
				for (InstructionFuture<JSON> i : ch) {
					args.add(i.call(context, t));
				}
				return transform(allAsList(args),
						new AsyncFunction<List<JSON>, JSON>() {
							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws Exception {
								JSONArray arr = builder.array(null,
										input.size());
								for (JSON j : input) {
									arr.add(j == null ? builder.value() : j);
								}
								return immediateFuture(arr);
							}
						});
			}
		};
	}

	public InstructionFuture<JSON> dyadic(InstructionFuture<JSON> left,
			InstructionFuture<JSON> right, DyadicAsyncFunction<JSON> f) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> parent)
					throws ExecutionException {
				return transform(
						allAsList(left.call(context, parent),
								right.call(context, parent)),
						new KeyedAsyncFunction<List<JSON>, JSON, DyadicAsyncFunction<JSON>>(
								f) {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws ExecutionException {
								Iterator<JSON> it = input.iterator();
								JSON l = it.next();
								JSON r = it.next();
								if (l == null || r == null)
									return immediateFuture(null);

								try {
									return immediateFuture(k.invoke(context, l,
											r));
								} catch (ExecutionException e) {
									return immediateFailedFuture(e);
								}
							}
						});
			}
		};

	}

	static class ObjectInstructionFuture extends
			AbstractInstructionFuture<JSON> {
		final InstructionFutureFactory factory;
		// protected Set<String> keys = new HashSet<>();
		final boolean isContextObject;
		final List<Pair<String, InstructionFuture<JSON>>> ll;
		final JSONBuilder builder;

		protected ListenableFuture<JSON> loadConfig() {
			File f = new File("jtlconfig.json");
			if (f.exists()) {
				try {
					JSON j = builder.parse(new FileInputStream(f));
					if (j == null)
						return Futures
								.immediateFailedCheckedFuture(new ExecutionException(
										"json parse failed"));
					return Futures.immediateFuture(j);
				} catch (IOException e) {
					return Futures
							.immediateFailedCheckedFuture(new ExecutionException(
									e));
				}
			} else {
				System.err.println("config not found");
			}
			return Futures.immediateFuture(builder.value());
		}

		public ObjectInstructionFuture(InstructionFutureFactory factory,
				final List<Pair<String, InstructionFuture<JSON>>> ll,
				JSONBuilder builder, boolean isContextObject) {
			this.factory = factory;
			this.builder = builder;
			this.isContextObject = isContextObject;
			this.ll = ll;
		}

		protected ListenableFuture<JSON> contextObject(
				final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> data) throws ExecutionException {
			InstructionFuture<JSON> defaultInstruction = null;
			InstructionFuture<JSON> init = null;
			List<InstructionFuture<JSON>> imperitives = new ArrayList<>(
					ll.size());
			for (Pair<String, InstructionFuture<JSON>> ii : ll) {
				final String k = ii.f;
				final InstructionFuture<JSON> inst = ii.s;

				if (k.equals("!init")) {
					init = memo(inst);
					context.define("init", inst);
					init.call(context, loadConfig());
				} else if (k.equals("_")) {
					defaultInstruction = inst;
				} else if (k.startsWith("!")) {
					// variable, (almost) immediate evaluation
					InstructionFuture<JSON> imp = factory.deferred(inst,
							context, data);
					context.define(k.substring(1), imp);
					imperitives.add(imp);
				} else if (k.startsWith("$")) {
					// variable, deferred evaluation
					context.define(k.substring(1),
							factory.deferred(inst, context, data));
				} else {
					// define a function
					context.define(k, inst);
				}
			}
			for (InstructionFuture<JSON> imp : imperitives) {
				imp.call(context, data);
			}
			return defaultInstruction.call(context, data);
		}

		protected ListenableFuture<JSON> dataObject(
				final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> data) throws ExecutionException {
			List<ListenableFuture<Pair<String, JSON>>> insts = new ArrayList<>(
					ll.size());
			for (Pair<String, InstructionFuture<JSON>> ii : ll) {
				final String kk = ii.f;
				ListenableFuture<Pair<String, JSON>> lf = transform(
						ii.s.call(context, data),
						new AsyncFunction<JSON, Pair<String, JSON>>() {

							@Override
							public ListenableFuture<Pair<String, JSON>> apply(
									JSON input) throws Exception {
								input.setName(kk);
								return immediateFuture(new Pair(kk, input));
							}
						});
				insts.add(lf);
			}
			return transform(allAsList(insts),
					new AsyncFunction<List<Pair<String, JSON>>, JSON>() {
						@Override
						public ListenableFuture<JSON> apply(
								List<Pair<String, JSON>> input)
								throws Exception {
							JSONObject obj = builder.object(null, input.size());

							for (Pair<String, JSON> d : input) {
								obj.put(d.f,
										d.s != null ? d.s : builder.value());
							}
							return immediateFuture(obj);
						}
					});
		}

		@Override
		public ListenableFuture<JSON> call(
				final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> data) throws ExecutionException {
			final AsyncExecutionContext<JSON> childContext = isContextObject ? context
					.createChild(false) : context;
			if (isContextObject) {
				return contextObject(childContext, data);
			} else {
				return dataObject(childContext, data);
			}
		}

	}

	public InstructionFuture<JSON> stepParent() {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						JSON p = input.getParent();
						JSON res = p == null ? builder.value() : p;
						return immediateFuture(res);
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> stepSelf() {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return data;
			}
		};
	}

	public InstructionFuture<JSON> recursDown() {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					protected void recurse(JSONArray unbound, JSON j) {
						JSONType type = j.getType();
						switch (type) {
						case ARRAY: {
							JSONArray a = (JSONArray) j;
							for (JSON jj : a) {
								unbound.add(jj);
								recurse(unbound, jj);
							}
						}
							break;
						case OBJECT:
							JSONObject a = (JSONObject) j;
							for (Pair<String, JSON> jj : a) {
								unbound.add(jj.s);
								recurse(unbound, jj.s);
							}
						}

					}

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						if (input == null)
							return immediateFuture(null);
						JSONArray unbound = builder.array(input, false);
						unbound.add(input);
						recurse(unbound, input);
						return immediateFuture(unbound);
					}
				});
			}
		};
	}


	// cache
	public InstructionFuture<JSON> recursUp() {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					protected void recurse(JSONArray unbound, JSON j) {
						if (j == null)
							return;
						JSON p = j.getParent();
						if (p != null) {
							recurse(unbound, p);
							unbound.add(p);
						}
					}

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						JSONArray unbound = builder.array(input, false);
						recurse(unbound, input);
						return immediateFuture(unbound);
					}
				});
			}
		};
	}

	// cache
	public InstructionFuture<JSON> stepChildren() {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						JSONType type = input.getType();
						switch(type) {
						case ARRAY:{
							JSONArray unbound = builder.array(input, false);
							JSONArray arr = (JSONArray) input;
							for (JSON j : arr) {
								unbound.add(j);
							}
							return immediateFuture(input);
						}
						case OBJECT:{
							JSONArray unbound = builder.array(input, false);
							JSONObject obj = (JSONObject) input;
							for (Pair<String, JSON> ee : obj) {
								unbound.add(ee.s);
							}
							return immediateFuture(unbound);
						}
						default:
							JSONArray unbound = builder.array(input, false);
							return immediateFuture(unbound);
						}
					}
				});
			}
		};
	}
	public InstructionFuture<JSON> string(final String label) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return immediateFuture(builder.value(label));
			}
		};
	}
	public InstructionFuture<JSON> get(String label) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						if (input == null)
							return immediateFuture(null);
						JSONType type = input.getType();
						if (type == JSONType.OBJECT) {
							JSONObject obj = (JSONObject) input;
							JSON r = obj.get(label);
							r = r == null ? builder.value() : r;
							return immediateFuture(r);
						}
						return immediateFuture(builder.value());
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> ternary(final InstructionFuture<JSON> c,
			final InstructionFuture<JSON> a, final InstructionFuture<JSON> b) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				return transform(c.call(context, data),
						new AsyncFunction<JSON, JSON>() {

							@Override
							public ListenableFuture<JSON> apply(JSON input)
									throws Exception {
								return input.isTrue() ? a.call(context, data)
										: b.call(context, data);
							}
						});
			}
		};
	}
	public InstructionFuture<JSON> object(
			final List<Pair<String, InstructionFuture<JSON>>> ll)
			throws ExecutionException {
		boolean isContext = false;
		for (Pair<String, InstructionFuture<JSON>> ii : ll) {
			if ("_".equals(ii.f)) {
				isContext = true;
				break;
			}
		}
		return new ObjectInstructionFuture(this, ll, builder, isContext);
	}

	public InstructionFuture<JSON> deindex(InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		return null;
	}

	public InstructionFuture<JSON> addInstruction(InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		return dyadic(a, b, new DefaultPolymorphicOperator(builder) {
			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return l + r;
			}

			@Override
			public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
				return l + r;
			}

			@Override
			public String op(AsyncExecutionContext<JSON> eng, String l, String r) {
				return l + r;
			}

			@Override
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l,
					JSONArray r) {
				// Collection<JSON> cc = builder.collection();
				JSONArray arr = builder.array(null);
				// this needs to be a deep clone for the internal referencing to
				// hold.
				int i = 0;
				for (JSON j : l.collection()) {
					arr.add(j);
				}
				for (JSON j : r.collection()) {
					arr.add(j);
				}
				return arr;
			}

			@Override
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l,
					JSON r) {
				JSONArray arr = builder.array(null);
				// this needs to be a deep clone for the internal referencing to
				// hold.
				int i = 0;
				for (JSON j : l.collection()) {
					arr.add(j);
				}
				arr.add(r);
				return arr;
			}

			@Override
			public JSONObject op(AsyncExecutionContext<JSON> eng, JSONObject l,
					JSONObject r) {
				JSONObject obj = builder.object(null);
				for (Map.Entry<String, JSON> ee : r.map().entrySet()) {
					String k = ee.getKey();
					JSON j = ee.getValue();
					obj.put(k, j);
				}
				for (Map.Entry<String, JSON> ee : l.map().entrySet()) {
					String k = ee.getKey();
					JSON j = ee.getValue();
					obj.put(k, j);
				}
				return obj;
			}
		});
	}

	public InstructionFuture<JSON> subInstruction(InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		return dyadic(a, b, new DefaultPolymorphicOperator(builder) {
			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return l - r;
			}

			@Override
			public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
				return l - r;
			}

			@Override
			public String op(AsyncExecutionContext<JSON> eng, String l, String r) {
				int n = l.indexOf(r);
				if (n != -1) {
					StringBuilder b = new StringBuilder(l.substring(0, n));
					b.append(l.substring(n + r.length()));
					return b.toString();
				}
				return r;
			}

			@Override
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l,
					JSONArray r) {
				JSONArray arr = builder.array(null);
				// this needs to be a deep clone for the internal referencing to
				// hold.
				for (JSON j : l.collection()) {
					if (!r.contains(j)) {
						arr.add(j);
					}
				}
				return arr;
			}

			@Override
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l,
					JSON r) {
				JSONArray arr = builder.array(null);
				// this needs to be a deep clone for the internal referencing to
				// hold.
				for (JSON j : l.collection()) {
					if (!j.equals(r)) {
						arr.add(j);
					}
				}
				return arr;
			}

			@Override
			public JSONObject op(AsyncExecutionContext<JSON> eng, JSONObject l,
					JSONObject r) {
				JSONObject obj = builder.object(null, l.size() + r.size());
				for (Map.Entry<String, JSON> ee : r.map().entrySet()) {
					String k = ee.getKey();
					JSON j = ee.getValue();
					if (!r.containsKey(k))
						obj.put(k, j);
				}
				return obj;
			}
		});
	}

	public InstructionFuture<JSON> mulInstruction(InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		return dyadic(a, b, new DefaultPolymorphicOperator(builder) {
			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return l * r;
			}

			@Override
			public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
				return l * r;
			}

		});

	}

	public InstructionFuture<JSON> divInstruction(InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		return dyadic(a, b, new DefaultPolymorphicOperator(builder) {
			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return l / r;
			}

			@Override
			public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
				return l / r;
			}
		});

	}

	public InstructionFuture<JSON> modInstruction(InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		return dyadic(a, b, new DefaultPolymorphicOperator(builder) {
			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return l % r;
			}

			@Override
			public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
				return l % r;
			}
		});
	}

}
