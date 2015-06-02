package org.dykman.jtl.core.engine.future;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.input.InputMethodHighlight;
import main.antlr.jtlParser.Add_exprContext;
import main.antlr.jtlParser.Mul_exprContext;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.Pair;
import org.dykman.jtl.core.engine.ExecutionException;
import org.dykman.jtl.core.parser.InstructionValue;

import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.util.concurrent.Futures.*;

public class InstructionFutureFactory {

	
	static InstructionFuture<JSON> NULLINST = null;
	static InstructionFuture<JSON> TRUEINST = null;
	static InstructionFuture<JSON> FALSEINST = null;

	final JSONBuilder builder;

	InstructionFutureFactory(JSONBuilder builder) {
		this.builder = builder;
	}

	public InstructionFuture<JSON> variable(final String name) {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) {
				try {
					return context.lookup(name);
				} catch (Exception e) {
					return immediateFailedCheckedFuture(e);
				}
			}
		};
	}

	public InstructionFuture<JSON> function(final String name,
			final List<InstructionFuture<JSON>> iargs) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) throws JSONException {
				AsyncExecutionContext<JSON> childContext = context.createChild();
				
				List<DeferredCall> a = new ArrayList<>(iargs.size());
				int cc = 0;
				for (InstructionFuture<JSON> i : iargs) {
					DeferredCall dc = new DeferredCall(i,context,t);
					childContext.setDeferred(Integer.toString(cc++), dc);
					a.add(new DeferredCall(i,context,t));
				}
				return childContext.call(name, t);
//				return context.call(name, a, t, context);
			}
		};
	}

	public InstructionFuture<JSON> value(final Boolean val) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) {
				return transform(t, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						return immediateFuture(builder.value(val));
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> value() {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) {
				return transform(t, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						return immediateFuture(builder.value());
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> value(final Long val) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) {
				return transform(t, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						return immediateFuture(builder.value(val));
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> value(final Double val) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) {
				return transform(t, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						return immediateFuture(builder.value(val));
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> value(final String val) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) {
				return transform(t, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						builder.value(val);
						return immediateFuture(builder.value(val));
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> nil() {
		return value();
	}

	public InstructionFuture<JSON> bool(final Boolean b) {
		return value(b);
	}

	public InstructionFuture<JSON> string(final String str) {
		return value(str);
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
					final ListenableFuture<JSON> t) throws JSONException {
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
								int i = 0;
								for (JSON j : input) {
									arr.add(j);
								}
								arr.lock();
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
					final ListenableFuture<JSON> parent) throws JSONException {
				return transform(
						allAsList(left.call(context, parent),
								right.call(context, parent)),
						new KeyedAsyncFunction<List<JSON>, JSON, DyadicAsyncFunction<JSON>>(
								f) {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws JSONException {
								Iterator<JSON> it = input.iterator();
								JSON l = it.next();
								JSON r = it.next();
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
		// protected Set<String> keys = new HashSet<>();
		final boolean isContextObject;
		final List<Duo<String, InstructionFuture<JSON>>> ll;
		final JSONBuilder builder;

		public ObjectInstructionFuture(
				final List<Duo<String, InstructionFuture<JSON>>> ll,
				JSONBuilder builder, boolean isContextObject) {
			this.isContextObject = isContextObject;
			this.ll = ll;
			this.builder = builder;
		}

		protected ListenableFuture<JSON> contextObject(
				final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> data) throws JSONException {

			final AsyncExecutionContext<JSON> childContext = context
					.createChild();
			InstructionFuture<JSON> defaultInstruction = null;
			InstructionFuture<JSON> init = null;
			for (Duo<String, InstructionFuture<JSON>> ii : ll) {
				String k = ii.first;
				final InstructionFuture<JSON> inst = ii.second;
				if (k.equals("init")) {
					init = inst;
				} else if (k.equals("_")) {
					defaultInstruction = inst;
				} else if (k.startsWith("!")) {
					// variable, immediate evaluation
					childContext.set(k, inst.call(childContext, data));
				} else if (k.startsWith("$")) {
					// variable, deferred evaluation
					childContext.setDeferred(k.substring(1),
						new DeferredCall(inst, childContext, data));
				} else {
					// define a function
					childContext.define(k, inst);
				}
			}
			if (init != null) {
				// init does not get access to input data
				ListenableFuture<JSON> inif = init.call(childContext,
						immediateFuture(builder.value()));
				childContext.set("init", inif);
				return transform(
						inif,
						new KeyedAsyncFunction<JSON, JSON, InstructionFuture<JSON>>(
								defaultInstruction) {
							@Override
							public ListenableFuture<JSON> apply(JSON input)
									throws Exception {
								if (input.getType() == JSONType.OBJECT) {
									for (Pair<String, JSON> it : (JSONObject) input) {
										childContext.set(it.f,
												immediateFuture(it.s));
									}
								}
								// execute the default instruction after init
								// has completed
								return k.call(childContext, data);
							}
						});
			}
			// call default instruction if no init has been specified
			return defaultInstruction.call(childContext, data);
		}

		protected ListenableFuture<JSON> dataObject(
				final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> data) throws JSONException {
			List<ListenableFuture<Duo<String, JSON>>> insts = new ArrayList<>(
					ll.size());
			for (Duo<String, InstructionFuture<JSON>> ii : ll) {
				final String kk = ii.first;
				ListenableFuture<Duo<String, JSON>> lf = transform(
						ii.second.call(context, data),
						new AsyncFunction<JSON, Duo<String, JSON>>() {

							@Override
							public ListenableFuture<Duo<String, JSON>> apply(
									JSON input) throws Exception {
								input.setName(kk);
								return immediateFuture(new Duo(kk, input));
							}
						});
				insts.add(lf);
			}
			return transform(allAsList(insts),
					new AsyncFunction<List<Duo<String, JSON>>, JSON>() {

						@Override
						public ListenableFuture<JSON> apply(
								List<Duo<String, JSON>> input) throws Exception {
							JSONObject obj = builder.object(null, input.size());

							for (Duo<String, JSON> d : input) {
								obj.put(d.first, d.second);
							}
							obj.lock();
							return immediateFuture(obj);
						}
					});
		}

		@Override
		public ListenableFuture<JSON> call(
				final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> data) throws JSONException {
			final AsyncExecutionContext<JSON> childContext = isContextObject ? context
					.createChild() : context;
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
					ListenableFuture<JSON> data) throws JSONException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						JSON p = input.getParent();
						JSON res = p == null ? input : p;
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
					ListenableFuture<JSON> data) throws JSONException {
				return data;
			}
		};
	}

	public InstructionFuture<JSON> recursDown() {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws JSONException {
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
						JSONArray unbound = builder.array(input, false);
						unbound.add(input);
						recurse(unbound, input);
						return immediateFuture(unbound);
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> get(String label) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws JSONException {
				return transform(data,new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						JSONType type = input.getType();
						if(type == JSONType.OBJECT) {
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
					ListenableFuture<JSON> data) throws JSONException {
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

	// cache
	public InstructionFuture<JSON> recursUp() {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws JSONException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					protected void recurse(JSONArray unbound, JSON j) {
						JSON p = j.getParent();
						if (p != null) {
							unbound.add(p);
							recurse(unbound, p);
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
					ListenableFuture<JSON> data) throws JSONException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						JSONType type = input.getType();
						if (type == JSONType.ARRAY) {
							return immediateFuture(input);
						} else if (type == JSONType.OBJECT) {
							JSONArray unbound = builder.array(input, false);
							JSONObject obj = (JSONObject) input;
							for (Pair<String, JSON> ee : obj) {
								unbound.add(ee.s);
							}
							return immediateFuture(unbound);
						} else {
							JSONArray unbound = builder.array(input, false);
							return immediateFuture(unbound);
						}
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> object(
			final List<Duo<String, InstructionFuture<JSON>>> ll)
			throws JSONException {
		boolean isContext = false;
		for (Duo<String, InstructionFuture<JSON>> ii : ll) {
			if ("_".equals(ii.first)) {
				isContext = true;
				break;
			}
		}
		return new ObjectInstructionFuture(ll, builder, isContext);
	}

	public InstructionFuture<JSON> __object(
			final List<Duo<String, InstructionFuture<JSON>>> ll)
			throws JSONException {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) throws JSONException {
				final JSONObject object = builder.object(null);
				List<ListenableFuture<JSON>> pp = new ArrayList<>(ll.size());
				for (Duo<String, InstructionFuture<JSON>> p : ll) {
					ListenableFuture<JSON> vl = p.second.call(context, t);
					pp.add(transform(
							vl,
							new KeyedPayloadAsyncFunction<JSON, JSON, String, JSONObject>(
									p.first, object) {
								@Override
								public ListenableFuture<JSON> apply(JSON input)
										throws Exception {
									input.setParent(object);
									p.put(k, input);
									return immediateFuture(input);
								}
							}));
				}
				return transform(allAsList(pp),
						new KeyedAsyncFunction<List<JSON>, JSON, JSONObject>(
								object) {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws Exception {
								return immediateFuture(k);
							}

						});
			}
		};
	}

	// public INstruc

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
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) { return l / r; }

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
