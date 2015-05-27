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

import org.antlr.v4.runtime.tree.TerminalNode;
import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.Pair;
import org.dykman.jtl.core.engine.ExecutionException;
import org.dykman.jtl.core.parser.InstructionValue;
import org.dykman.jtl.core.parser.JSONBuilder;

import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.util.concurrent.Futures.*;

public class InstructionFutureFactory {

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
				List<ListenableFuture<JSON>> a = new ArrayList<>(iargs.size());
				for (InstructionFuture<JSON> i : iargs) {
					a.add(i.call(context, t));
				}
				return context.call(name, a, t, context);
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
						return immediateFuture(new JSONValue(input, val));
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
						return immediateFuture(new JSONValue(input, (Long) null));
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
						return immediateFuture(new JSONValue(input, val));
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
						return immediateFuture(new JSONValue(input, val));
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
						return immediateFuture(new JSONValue(input, val));
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
		return value(i!=null ? Long.parseLong(i.getText()) : Double.parseDouble(d.getText()));
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
								Collection<JSON> cc = builder.collection(input.size());
								JSONArray arr = builder.array(null, cc);
								int i = 0;
								for (JSON j : input) {
									j.setParent(arr);
									j.setIndex(i++);
									j.lock();
									cc.add(j);
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

			final AsyncExecutionContext<JSON> childContext = context.createChild();
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
					childContext.set(k,inst.call(childContext, data));
				} else if (k.startsWith("$")) {
					// variable, deferred evaluation
					childContext.setDeferred(k.substring(1),
							new InstructionFuture<JSON>() {
								@Override
								public ListenableFuture<JSON> call(
										final AsyncExecutionContext<JSON> __context,
										final ListenableFuture<JSON> data)
										throws JSONException {
									return inst.call(childContext, data);
								}
							}, data);
				} else {
					// define a function
					childContext.define(k, inst);
				}
			}
			if (init != null) {
				// init does not get access to input data
				ListenableFuture<JSON> inif = init.call(childContext,immediateFuture(new JSONValue(null)));
				childContext.set("init", inif);
				return transform(inif,
						new KeyedAsyncFunction<JSON, JSON, InstructionFuture<JSON>>(
								defaultInstruction) {
							@Override
							public ListenableFuture<JSON> apply(JSON input)
									throws Exception {
								if(input.getType() == JSONType.OBJECT) {
									for(Pair<String,JSON> it : (JSONObject) input) {
										childContext.set(it.f, immediateFuture(it.s));
									}
								}
								// execute the default instruction after init has completed
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
			final Map<String, JSON> m = builder.map(ll.size());
			for (Duo<String, InstructionFuture<JSON>> ii : ll) {
				final String kk = ii.first;
				ListenableFuture<Duo<String, JSON>> lf = transform(
						ii.second.call(context, data),
						new AsyncFunction<JSON, Duo<String, JSON>>() {

							@Override
							public ListenableFuture<Duo<String, JSON>> apply(
									JSON input) throws Exception {
								return immediateFuture(new Duo(kk, input));
							}
						});
				insts.add(lf);
			}
			return transform(allAsList(insts),
					new AsyncFunction<List<Duo<String, JSON>>, JSON>() {

						@Override
						public ListenableFuture<JSON> apply(
								List<Duo<String, JSON>> input)
								throws Exception {
							final Map<String, JSON> m = builder.map(input
									.size());
							JSONObject obj = builder.object(null,m);

							for (Duo<String, JSON> d : input) {
								d.second.setParent(obj);
								d.second.setName(d.first);
								d.second.lock();
								m.put(d.first, d.second);
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
				final JSONObject object = (JSONObject) JSONObject.create(
						(JSON) null, context.engine());
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
}