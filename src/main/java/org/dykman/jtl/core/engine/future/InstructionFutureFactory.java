package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedCheckedFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.antlr.jsonParser.JsonContext;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.tool.BuildDependencyGenerator;
import org.dykman.jtl.core.Frame;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.JtlCompiler;
import org.dykman.jtl.core.ModuleLoader;
import org.dykman.jtl.core.Pair;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class InstructionFutureFactory {

	final JSONBuilder builder;

	protected static final String JTL_INTERNAL = "_jtl_internal_";
	protected static final String JTL_INTERNAL_KEY = JTL_INTERNAL + "key_";

	public InstructionFutureFactory(JSONBuilder builder) {
		this.builder = builder;
	}

	public static InstructionFuture<JSON> memo(final InstructionFuture<JSON> inst) {
		return new AbstractInstructionFuture() {
			private ListenableFuture<JSON> result = null;
			private boolean fired = false;

			@Override
			public InstructionFuture<JSON> unwrap() {
				return inst.unwrap();
			}

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
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
			
			
			/// DEAD
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return null;
			}
		};
	}

	public JSONBuilder builder() {
		return builder;
	}

	public InstructionFuture<JSON> file() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				InstructionFuture<JSON> f = context.getdef("1");
				return transform(f.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						Callable<JSON> cc = new Callable<JSON>() {

							@Override
							public JSON call() throws Exception {
								File ff = new File(stringValue(input));
								if (ff.exists())
									return builder.parse(ff);
								return builder.value();
							}
						};
						return context.executor().submit(cc);
					}
				});
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public InstructionFuture<JSON> map() {
		return new AbstractInstructionFuture() {
			InstructionFuture<JSON> mapfunc;

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				InstructionFuture<JSON> gbe = context.getdef("1");
				if (gbe != null) {
					mapfunc = gbe.unwrap();
				} else {
					return immediateCheckedFuture(builder.value());
				}

				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						switch (input.getType()) {
							case OBJECT: {
								JSONObject obj = (JSONObject) input;
								List<ListenableFuture<Pair<String, JSON>>> ll = new ArrayList<>();

								for (Pair<String, JSON> pp : obj) {
									AsyncExecutionContext<JSON> ctx = context.createChild(true);
									ctx.define("0", value("map"));
									ctx.define(JTL_INTERNAL_KEY, value(pp.f));
									final String kk = pp.f;
									ListenableFuture<JSON> remapped = mapfunc.call(context, immediateCheckedFuture(pp.s));
									ll.add(transform(remapped, new KeyedAsyncFunction<JSON, Pair<String, JSON>, String>(kk) {
										@Override
										public ListenableFuture<Pair<String, JSON>> apply(JSON input) throws Exception {
											return immediateCheckedFuture(new Pair<>(k, input));
										}
									}));

								}
								return transform(allAsList(ll), new AsyncFunction<List<Pair<String, JSON>>, JSON>() {

									@Override
									public ListenableFuture<JSON> apply(List<Pair<String, JSON>> input) throws Exception {
										JSONObject result = builder.object(null);
										for (Pair<String, JSON> pp : input) {
											result.put(pp.f, pp.s);
										}

										return immediateCheckedFuture(result);
									}
								});
							}
							case ARRAY:
							default:
								return immediateCheckedFuture(builder.value());
						}
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> groupBy() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return callItem(context, data);
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				InstructionFuture<JSON> gbe = context.getdef("1");
				if (gbe != null) {
					gbe = gbe.unwrap();
				} else {
					return data;
				}

				final InstructionFuture<JSON> filter = gbe;
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						// JSONObject obj = builder.object(null);
						JSONType type = input.getType();
						if (type != JSONType.ARRAY && type != JSONType.FRAME)
							return immediateCheckedFuture(builder.object(null));
						JSONArray array = (JSONArray) input;
						List<ListenableFuture<Pair<JSON, JSON>>> ll = new ArrayList<>();
						for (JSON j : array) {
							final JSON k = j;
							ll.add(transform(filter.call(context, immediateCheckedFuture(k)),
								new AsyncFunction<JSON, Pair<JSON, JSON>>() {
									public ListenableFuture<Pair<JSON, JSON>> apply(JSON inp) throws Exception {
										return immediateCheckedFuture(new Pair<>(k, inp));
									}
								}));

						}
						return transform(allAsList(ll), new AsyncFunction<List<Pair<JSON, JSON>>, JSON>() {

							@Override
							public ListenableFuture<JSON> apply(List<Pair<JSON, JSON>> input) throws Exception {
								JSONObject obj = builder.object(null);
								for (Pair<JSON, JSON> pp : input) {
									String s = stringValue(pp.s);
									JSONArray a = (JSONArray) obj.get(s);
									if (a == null) {
										a = builder.array(obj);
										obj.put(s, a, true);
									}
									a.add(pp.f);
								}
								return immediateCheckedFuture(obj);
							}
						});
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> importInstruction(JSON conf) {
		return new AbstractInstructionFuture() {

			protected ListenableFuture<JSON> loadJtl(final AsyncExecutionContext<JSON> context, final String file) {
				final AsyncExecutionContext<JSON> ctx = context.getMasterContext();
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				Callable<JSON> cc = new Callable<JSON>() {

					@Override
					public JSON call() throws Exception {
						final JtlCompiler compiler = new JtlCompiler(builder, false, false, true);
						InstructionFuture<JSON> inst = compiler.parse(new File(file));
						return inst.call(ctx, immediateCheckedFuture(conf)).get();
					}
				};
				return context.executor().submit(cc);
			}

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSONType type = input.getType();
						if (type == JSONType.STRING) {
							return loadJtl(context, stringValue(input));
						}
						if (type == JSONType.ARRAY) {
							List<ListenableFuture<JSON>> ll = new ArrayList<>();
							for (JSON j : (JSONArray) input) {
								if (j.getType() == JSONType.STRING) {
									ll.add(loadJtl(context, stringValue(j)));
								}
							}
							return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
								@Override
								public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
									JSONArray arr = builder.array(null);
									for (JSON j : input) {
										arr.add(j);
									}
									return immediateCheckedFuture(arr);
								}
							});
						}
						return immediateCheckedFuture(builder.value());
					}
				});
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return null;
			}
		};
	}

	public InstructionFuture<JSON> loadModule(final JSONObject conf) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>(3);
				InstructionFuture<JSON> key = context.getdef(JTL_INTERNAL_KEY);
				InstructionFuture<JSON> name = context.getdef("1");
				if (key != null) {
					ll.add(key.call(context, data));
				} else {
					ll.add(name.call(context, data));
				}
				ll.add(name.call(context, data));
				InstructionFuture<JSON> ci = context.getdef("2");
				if (ci != null) {
					ll.add(ci.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						String key = stringValue(jit.next());

						String name = stringValue(jit.next());
						JSONObject config = (JSONObject) (jit.hasNext() ? jit.next() : null);
						ModuleLoader ml = ModuleLoader.getInstance(builder, conf);
						AsyncExecutionContext<JSON> modctx = context.getMasterContext().getNamedContext(key);
						int n = ml.create(name, modctx, config);
						return immediateCheckedFuture(builder.value(n));
					}
				});

			}
		};

	}

	private JSON applyRegex(Pattern p, JSON j) {
		if (j.isValue()) {
			String ins = ((JSONValue) j).stringValue();
			if (ins != null) {
				Matcher m = p.matcher(ins);
				if (m.find()) {
					JSONArray unbound = builder.array(null);
					int n = m.groupCount();
					for (int i = 0; i <= n; ++i) {
						JSON r = builder.value(m.group(i));
						unbound.add(r);
					}
					return unbound;
				}
			}
		}
		if (j.getType() == JSONType.ARRAY) {
			JSONArray unbound = builder.array(null);
			JSONArray inarr = (JSONArray) j;
			boolean any = false;
			for (JSON k : inarr) {
				JSON r = applyRegex(p, k);
				any |= r.isTrue();
				unbound.add(r);
			}
			return any ? unbound : builder.array(null);
		}
		if (j.getType() == JSONType.OBJECT) {
			JSONObject unbound = builder.object(null);
			JSONObject inarr = (JSONObject) j;
			for (Pair<String, JSON> jj : inarr) {
				JSON r = applyRegex(p, jj.s);
				if (r.isTrue())
					unbound.put(jj.f, r);
			}
			return unbound;
		}
		return builder.value();

	}

	public InstructionFuture<JSON> reMatch(final String p, final InstructionFuture<JSON> d) {
		final Pattern pattern = Pattern.compile(p);
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(d.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return immediateCheckedFuture(applyRegex(pattern, input));
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> negate(InstructionFuture<JSON> ii) {
		// TODO:: need to actually apply negation here: boring but eventually
		// necessary
		return ii;
	}

	public InstructionFuture<JSON> variable(final String name) {
		return new AbstractInstructionFuture() {
			/// DEAD
			@Override
			public ListenableFuture<JSON> callItem(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t) {
				return null;
			}
			@Override
			public ListenableFuture<JSON> call(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t) {
				try {
					// if (t == null)
					// return immediateCheckedFuture(builder.value());
					// System.err.println("variable: " + name);
					return context.lookup(name, t);
				} catch (Exception e) {
					return immediateFailedCheckedFuture(new ExecutionException(e));
				}
			}
		};
	}

	public InstructionFuture<JSON> deferred(InstructionFuture<JSON> inst, AsyncExecutionContext<JSON> context,
		final ListenableFuture<JSON> t) {
		return memo(new DeferredCall(inst, context, t));
	}

	public InstructionFuture<JSON> function(final String name, final List<InstructionFuture<JSON>> iargs) {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> call(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t)
				throws ExecutionException {
				return callItem(context, t);
			}

			@Override
			public ListenableFuture<JSON> callItem(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t)
				throws ExecutionException {
				// System.err.println("calling function '" + name + "'.");
				InstructionFuture<JSON> func = context.getdef(name);
				if (func == null) {
					System.err.println("function '" + name + "' not found.");
					return immediateCheckedFuture(null);
				}
				// a function context has numeric-labeled functions in its table
				// which should not fall back to the parent
				AsyncExecutionContext<JSON> childContext = context.createChild(true);

				List<InstructionFuture<JSON>> a = new ArrayList<>(iargs.size());
				int cc = 1;
				childContext.define("0", value(name));
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
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return o;
			}
		};
	}

	public static InstructionFuture<JSON> value(JSON o) {
		return value(immediateCheckedFuture(o));
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
		if (i != null)
			return value(Long.parseLong(i.getText()));
		return value(Double.parseDouble(d.getText()));
	}

	public InstructionFuture<JSON> array(final List<InstructionFuture<JSON>> ch) {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> callItem(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t)
				throws ExecutionException {
				List<ListenableFuture<JSON>> args = new ArrayList<>();
				for (InstructionFuture<JSON> i : ch) {
					args.add(i.call(context, t));
				}
				return transform(allAsList(args), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSONArray arr = builder.array(null, input.size());
						for (JSON j : input) {
							arr.add(j == null ? builder.value() : j);
						}
						return immediateCheckedFuture(arr);
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> dyadic(InstructionFuture<JSON> left, InstructionFuture<JSON> right,
		DyadicAsyncFunction<JSON> f) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> parent) throws ExecutionException {
				return transform(allAsList(left.call(context, parent), right.call(context, parent)),
					new KeyedAsyncFunction<List<JSON>, JSON, DyadicAsyncFunction<JSON>>(f) {

						@Override
						public ListenableFuture<JSON> apply(List<JSON> input) throws ExecutionException {
							Iterator<JSON> it = input.iterator();
							JSON l = it.next();
							JSON r = it.next();
							if (l == null || r == null)
								return immediateCheckedFuture(null);

							try {
								return immediateCheckedFuture(k.invoke(context, l, r));
							} catch (ExecutionException e) {
								return immediateFailedFuture(e);
							}
						}
					});
			}
		};

	}

	public InstructionFuture<JSON> unique() {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						switch (input.getType()) {
							case FRAME:
							case ARRAY: {
								Collection<JSON> cc = ((JSONArray) input).collection();
								Set<JSON> ss = new LinkedHashSet<>();
								for (JSON j : cc) {
									ss.add(j);
								}
								return immediateCheckedFuture(builder.array((JSON) null, ss));

							}
							default:
								return immediateCheckedFuture(input.cloneJSON());
						}
					}

				});
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public InstructionFuture<JSON> count() {
		return new AbstractInstructionFuture() {
			JSON cnt(JSON j) {
				switch (j.getType()) {
					case FRAME: {
						Frame f = builder.frame();
						for (JSON jj : (Frame) j) {
							f.add(cnt(jj));
						}
						return f;
					}
					case ARRAY:
						return builder.value(((JSONArray) j).collection().size());
					case OBJECT:
						return builder.value(((JSONObject) j).map().size());
					default:
						return builder.value(1);
				}

			}

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				InstructionFuture<JSON> arg = context.getdef("1");
				if (arg != null) {
					data = arg.call(context, data);
				}
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return immediateCheckedFuture(cnt(input));
					}
				});
			}

			// deadn
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public InstructionFuture<JSON> sequence(final InstructionFuture<JSON>... inst) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				for (InstructionFuture<JSON> ii : inst) {
					ll.add(ii.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Frame f = builder.frame();
						for (JSON j : input) {
							f.add(j);
						}
						return immediateCheckedFuture(f);
					}
				});
			}

			// / DEAD
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public InstructionFuture<JSON> params() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				boolean done = false;

				List<InstructionFuture<JSON>> ll = new ArrayList<>();
				for (int i = 1; done == false && i < 4096; ++i) {
					InstructionFuture<JSON> ci = context.getdef(Integer.toString(i));
					if (ci == null)
						done = true;
					else {
						ll.add(ci);
					}
				}
				return sequence(ll.toArray(new InstructionFuture[ll.size()])).call(context, data);
			}

			// / DEAD
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return null;
			}
		};

	}

	public InstructionFuture<JSON> defaultError() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				final InstructionFuture<JSON> first = context.getdef("1");
				final InstructionFuture<JSON> second = context.getdef("2");
				if (first == null) {
					JSONObject obj = builder.object(null);
					obj.put("status", builder.value(500L));
					obj.put("message", builder.value("unknown error"));
					return immediateCheckedFuture(obj);
				}
				final List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(first.call(context, data));
				if (second != null) {
					ll.add(second.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSONObject obj = builder.object(null);
						Iterator<JSON> jit = input.iterator();
						JSON f = jit.next();
						JSON s = jit.hasNext() ? jit.next() : null;
						if (f.isNumber()) {
							obj.put("status", builder.value(((JSONValue) f).longValue()));
							if (s != null) {
								obj.put("message", builder.value(((JSONValue) s).stringValue()));
							} else {
								obj.put("message", builder.value("an unknown error has occurred"));

							}
						} else {
							obj.put("status", builder.value(500L));
							obj.put("message", builder.value(((JSONValue) f).stringValue()));

						}
						return immediateCheckedFuture(obj);
					}
				});
			}
		};
	}

	class ObjectInstructionFuture extends AbstractInstructionFuture {
		final InstructionFutureFactory factory;
		// protected Set<String> keys = new HashSet<>();
		final List<Pair<String, InstructionFuture<JSON>>> ll;
		final JSONBuilder builder;

		public ObjectInstructionFuture(InstructionFutureFactory factory,
			final List<Pair<String, InstructionFuture<JSON>>> ll, JSONBuilder builder) {
			this.factory = factory;
			this.builder = builder;
			this.ll = ll;
		}

		protected ListenableFuture<JSON> dataObject(final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data) throws ExecutionException {
			List<ListenableFuture<Pair<String, JSON>>> insts = new ArrayList<>(ll.size());
			for (Pair<String, InstructionFuture<JSON>> ii : ll) {
				final String kk = ii.f;
				final AsyncExecutionContext<JSON> newc = context.createChild(false);
				newc.define(JTL_INTERNAL_KEY, value(kk));
				ListenableFuture<Pair<String, JSON>> lf = transform(ii.s.call(newc, data),
					new AsyncFunction<JSON, Pair<String, JSON>>() {
						@Override
						public ListenableFuture<Pair<String, JSON>> apply(JSON input) throws Exception {
							input.setName(kk);
							return immediateCheckedFuture(new Pair(kk, input));
						}
					});
				insts.add(lf);
			}

			return transform(allAsList(insts), new AsyncFunction<List<Pair<String, JSON>>, JSON>() {
				@Override
				public ListenableFuture<JSON> apply(List<Pair<String, JSON>> input) throws Exception {
					JSONObject obj = builder.object(null, input.size());

					for (Pair<String, JSON> d : input) {
						obj.put(d.f, d.s != null ? d.s : builder.value());
					}
					return immediateCheckedFuture(obj);
				}
			});
		}

		@Override
		public ListenableFuture<JSON> callItem(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> data)
			throws ExecutionException {
			return dataObject(context, data);
		}

	}

	class ContextObjectInstructionFuture extends AbstractInstructionFuture {
		final InstructionFutureFactory factory;
		final List<Pair<String, InstructionFuture<JSON>>> ll;
		final JSONBuilder builder;
		final boolean imported;

		public ContextObjectInstructionFuture(InstructionFutureFactory factory,
			final List<Pair<String, InstructionFuture<JSON>>> ll, JSONBuilder builder, boolean imported) {
			this.factory = factory;
			this.builder = builder;
			this.ll = ll;
			this.imported = imported;
		}

		protected ListenableFuture<JSON> contextObject(final AsyncExecutionContext<JSON> ctx,
			final ListenableFuture<JSON> data) throws ExecutionException {
			InstructionFuture<JSON> defaultInstruction = null;
			InstructionFuture<JSON> init = null;
			List<InstructionFuture<JSON>> imperitives = new ArrayList<>(ll.size());
			final AsyncExecutionContext<JSON> context = imported ? ctx.getMasterContext() : ctx.createChild(false);

			for (Pair<String, InstructionFuture<JSON>> ii : ll) {
				final String k = ii.f;
				final InstructionFuture<JSON> inst = ii.s;

				if (k.equals("!init")) {
					init = memo(inst);
					context.define("init", inst);
				} else if (k.equals("_")) {
					defaultInstruction = inst;
				} else if (k.startsWith("!")) {
					// variable, (almost) immediate evaluation
					InstructionFuture<JSON> imp = memo(inst);
					context.define(k.substring(1), imp);
					imperitives.add(imp);
				} else if (k.startsWith("$")) {
					// variable, deferred evaluation
					context.define(k.substring(1), factory.deferred(inst, context, data));
				} else {
					// define a function
					context.define(k, inst);
				}
			}
			try {
				// ensure that init is completed so that any modules are
				// installed
				// and imports imported
				final InstructionFuture<JSON> finst = defaultInstruction;
				AsyncFunction<List<JSON>, JSON> runner = new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						return finst != null && imported == false ? finst.call(context, data) : immediateCheckedFuture(builder
							.value(true));
					}
				};
				if (init != null) {
					return transform(init.call(context, context.config()), new AsyncFunction<JSON, JSON>() {
						@Override
						public ListenableFuture<JSON> apply(JSON input) throws Exception {
							// input is the result of init, don't care,
							// really
							List<ListenableFuture<JSON>> ll = new ArrayList<>();
							for (InstructionFuture<JSON> imp : imperitives) {
								ll.add(imp.call(context, data));
							}
							if (!ll.isEmpty())
								return transform(allAsList(ll), runner);
							if (finst != null)
								return finst.call(context, data);
							return immediateCheckedFuture(builder.value(true));
						}
					});
				}

				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				for (InstructionFuture<JSON> imp : imperitives) {
					ll.add(imp.call(context, data));
				}
				if (!ll.isEmpty())
					return transform(allAsList(ll), runner);
				if (finst != null)
					return finst.call(context, data);
				return immediateCheckedFuture(builder.value(true));
			} catch (ExecutionException e) {
				InstructionFuture<JSON> error = context.getdef("error");
				if (error == null)
					System.err.println("WTF!!!!????");
				AsyncExecutionContext<JSON> ec = context.createChild(true);
				ec.define("0", value("error"));
				ec.define("1", value(500L));
				ec.define("2", value(e.getLocalizedMessage()));
				return error.call(ec, data);
			}
		}

		@Override
		public ListenableFuture<JSON> callItem(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> data)
			throws ExecutionException {
			return contextObject(context.createChild(false), data);
		}

	}

	public InstructionFuture<JSON> stepParent() {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSON p = input.getParent();
						JSON res = p == null ? builder.value() : p.cloneJSON();
						return immediateCheckedFuture(res);
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> filter() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						InstructionFuture<JSON> fexp = context.getdef("1");
						if (fexp != null) {
							fexp = fexp.unwrap();
							List<ListenableFuture<JSON>> ll = new ArrayList<>();
							if (input instanceof Frame || input instanceof JSONArray) {
								for (JSON j : (JSONArray) input) {
//									Pair<JSON,JSON> pp = new Pair<JSON, JSON>(j, s)
									ListenableFuture<JSON> jj = immediateCheckedFuture(j);
									ll.add(transform(fexp.call(context, jj), new KeyedAsyncFunction<JSON, JSON, JSON>(j) {

										@Override
										public ListenableFuture<JSON> apply(JSON input) throws Exception {
											if(input == null || input.isTrue() == false) {
												return immediateCheckedFuture(builder.value());
											}
											return immediateCheckedFuture(k);
										}
									}));
								}
							} else {
								ListenableFuture<JSON> jj = immediateCheckedFuture(input);
								ll.add(transform(fexp.call(context, jj), new KeyedAsyncFunction<JSON, JSON, JSON>(input) {

									@Override
									public ListenableFuture<JSON> apply(JSON input) throws Exception {
										if(input == null || input.isTrue() == false) {
											return immediateCheckedFuture(builder.value());
										}
										return immediateCheckedFuture(k);
									}
								}));
						}
						if(ll.size() == 0) return immediateCheckedFuture(builder.value());
							return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
									Frame frame = builder.frame();
									if(input.size() == 1) {
										Iterator<JSON> jit = input.iterator();
										JSON j = jit.next();
										if(j!=null&&j.isTrue()) {
											frame.add(j);
										}
									} else {
										for(JSON j:input) {
											if(j!=null&&j.isTrue()) {
												frame.add(j);
											}
										}
									}
									return immediateCheckedFuture(frame);
								}
							});
						} else {
							return immediateCheckedFuture(input);
						}
					}
				});
			}

			// / DEAD
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	public InstructionFuture<JSON> collate() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSONType type = input.getType();
						if(type == JSONType.ARRAY||type == JSONType.FRAME) {
							Map<String,Collection<JSON>> cols = new LinkedHashMap<>();
							int c = 0;
							for(JSON j:(JSONArray) input) {
								if(j.getType() == JSONType.OBJECT) {
									for(Pair<String,JSON> pp : (JSONObject) j) {
										Collection<JSON> col = cols.get(pp.f);
										if(col == null) {
											col = new ArrayList<>();
											cols.put(pp.f, col);
										}
										int n = c - col.size();
										if(n>0) {
											for(int i = 0;i < n; ++i) {
												col.add(builder.value());
											}
										}
										col.add(pp.s);
										
										
									}
								}
							}
							JSONObject obj = builder.object(null);
							for(Map.Entry<String, Collection<JSON>> ee : cols.entrySet()) {
								obj.put(ee.getKey(), builder.array(null, ee.getValue()));
							}
							return immediateCheckedFuture(obj);
						}
						return immediateCheckedFuture(input);
					}
				});
			}
			
			/// DEAD
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
	}
	public InstructionFuture<JSON> sort() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					Collection<JSON> sort(Collection<JSON> c, InstructionFuture<JSON> filter) {
						Map<JSON, JSON> trans = new HashMap<>();
						ArrayList<JSON> al = (c instanceof ArrayList) ? (ArrayList<JSON>) c : new ArrayList<>();

						Collections.sort(al, new Comparator<JSON>() {

							@Override
							public int compare(JSON o1, JSON o2) {
								if (filter != null) {
									JSON a = trans.get(o1);
									try {
										if (a == null) {
											a = filter.unwrap().call(context, immediateCheckedFuture(o1)).get();
											trans.put(o1, a);
										}
										JSON b = trans.get(o2);
										if (b == null) {
											b = filter.unwrap().call(context, immediateCheckedFuture(o2)).get();
											trans.put(o2, b);
										}
										return a.compareTo(b);
									} catch (ExecutionException | InterruptedException | java.util.concurrent.ExecutionException e) {

									}
								}
								return o1.compareTo(o2);

							}
						});
						return c;
					}

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input.getType() == JSONType.ARRAY || input.getType() == JSONType.FRAME) {
							Collection<JSON> content = ((JSONArray) input).collection();
							content = sort(content, context.getdef("1"));
							return immediateCheckedFuture(builder.array(null, content));

						} else {
							return immediateCheckedFuture(input);
						}
					}

				});
			}

			// / DEAD
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}

		};
	}

	public InstructionFuture<JSON> stepSelf() {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return callItem(context, data);
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return immediateCheckedFuture(input.cloneJSON());
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> recursDown() {
		return new AbstractInstructionFuture() {
			protected void recurse(Frame unbound, JSON j) {
				JSONType type = j.getType();
				switch (type) {
					case ARRAY: {
						JSONArray a = (JSONArray) j;
						for (JSON jj : a) {
							unbound.add(jj.cloneJSON());
							recurse(unbound, jj);
						}
					}
						break;
					case OBJECT:
						JSONObject a = (JSONObject) j;
						for (Pair<String, JSON> jj : a) {
							unbound.add(jj.s);
							recurse(unbound, jj.s.cloneJSON());
						}
				}
			}

			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						Frame unbound = builder.frame();
						switch (input.getType()) {
							case ARRAY: {
								JSONArray arr = (JSONArray) input;

								for (JSON j : arr) {
									unbound.add(j);
									recurse(unbound, j);
								}
							}
							case OBJECT: {
								JSONObject arr = (JSONObject) input;

								for (Pair<String, JSON> jj : arr) {
									unbound.add(jj.s);
									recurse(unbound, jj.s);
								}
							}
						}
						return immediateCheckedFuture(unbound);
					}
				});
				// return callItem(context, data);
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					protected void recurse(Frame unbound, JSON j) {
						JSONType type = j.getType();
						switch (type) {
							case ARRAY: {
								JSONArray a = (JSONArray) j;
								for (JSON jj : a) {
									unbound.add(jj.cloneJSON());
									recurse(unbound, jj);
								}
							}
								break;
							case OBJECT:
								JSONObject a = (JSONObject) j;
								for (Pair<String, JSON> jj : a) {
									unbound.add(jj.s);
									recurse(unbound, jj.s.cloneJSON());
								}
						}
					}

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input == null)
							return immediateCheckedFuture(null);
						Frame unbound = builder.frame();
						unbound.add(input);
						recurse(unbound, input);
						return immediateCheckedFuture(unbound);
					}
				});
			}
		};
	}

	// cache
	public InstructionFuture<JSON> recursUp() {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					protected void recurse(Frame unbound, JSON j) {
						if (j == null)
							return;
						JSON p = j.getParent();
						if (p != null) {
							recurse(unbound, p);
							unbound.add(p);
						}
					}

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						Frame unbound = builder.frame();
						recurse(unbound, input);
						return immediateCheckedFuture(unbound);
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> mapChildren() {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						switch (input.getType()) {
							case FRAME:
								// return callSuper(context, data);
							case ARRAY: {
								Frame frame = builder.frame();
								for (JSON j : (JSONArray) input) {
									frame.add(j);
								}
								return immediateCheckedFuture(frame);

							}
							case OBJECT: {
								JSONObject obj = builder.object(null);
								for (Pair<String, JSON> j : (JSONObject) input) {
									obj.put(j.f, j.s);
								}
								return immediateCheckedFuture(obj);

							}
							default:
						}
						return immediateCheckedFuture(builder.value());

					}
				});
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	// cache
	public InstructionFuture<JSON> stepChildren() {

		return new AbstractInstructionFuture() {

			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						switch (input.getType()) {
							case FRAME:
								return immediateCheckedFuture(input);
								// return callSuper(context, data);
							case ARRAY: {
								Frame frame = builder.frame();
								for (JSON j : (JSONArray) input) {
									frame.add(j);
								}
								return immediateCheckedFuture(frame);

							}
							case OBJECT: {
								Frame frame = builder.frame();
								for (Pair<String, JSON> j : (JSONObject) input) {
									frame.add(j.s);
								}
								return immediateCheckedFuture(frame);

							}
							default:
						}
						return immediateCheckedFuture(builder.value());

					}
				});
			}

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSONType type = input.getType();
						switch (type) {
							case ARRAY: {
								Frame unbound = builder.frame();
								JSONArray arr = (JSONArray) input;
								for (JSON j : arr) {
									unbound.add(j);
								}
								return immediateCheckedFuture(input);
							}
							case OBJECT: {
								Frame unbound = builder.frame();
								JSONObject obj = (JSONObject) input;
								for (Pair<String, JSON> ee : obj) {
									unbound.add(ee.s);
								}
								return immediateCheckedFuture(unbound);
							}
							default:
								Frame unbound = builder.frame();
								return immediateCheckedFuture(unbound);
						}
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> string(final String label) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return immediateCheckedFuture(builder.value(label));
			}
		};
	}

	public InstructionFuture<JSON> get(final String label) {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return callItem(context, data);
			}

			@Override
			public ListenableFuture<JSON> callItem(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					ListenableFuture<JSON> get(JSONObject j) throws ExecutionException {
						JSON r = j.get(label);
						if (!(r == null || r.getType() == JSONType.NULL)) {
							return immediateCheckedFuture(r.cloneJSON());
						}
						InstructionFuture<JSON> inst = context.getdef(label);
						if (inst != null) {
							return inst.call(context, data);
						}
						return null;
					}

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						switch (input.getType()) {
							case OBJECT: {
								JSONObject obj = (JSONObject) input;
								ListenableFuture<JSON> lf = get(obj);
								if (lf != null)
									return lf;
								return immediateCheckedFuture(builder.value());
							}
							case FRAME: {
								JSONArray arr = (JSONArray) input;
								List<ListenableFuture<JSON>> ll = new ArrayList<>();
								for (JSON j : arr) {

									if (j.getType() == JSONType.OBJECT) {
										JSONObject obj = (JSONObject) j;
										ListenableFuture<JSON> res = get(obj);
										if (res != null)
											ll.add(res);
									}
								}
								if (ll.size() > 0)
									return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

										@Override
										public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
											Frame unbound = builder.frame();
											for (JSON j : input) {
												unbound.add(j.cloneJSON());
											}
											return immediateCheckedFuture(unbound);
										}
									});
								return immediateCheckedFuture(builder.value());
							}

							case ARRAY:
							default:
								InstructionFuture<JSON> inst = context.getdef(label);
								if (inst != null) {
									return inst.call(context, data);
								}
								return immediateCheckedFuture(builder.value());
						}
						// return immediateCheckedFuture(builder.value());
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> ternary(final InstructionFuture<JSON> c, final InstructionFuture<JSON> a,
		final InstructionFuture<JSON> b) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(c.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return input.isTrue() ? a.call(context, data) : b.call(context, data);
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> object(final List<Pair<String, InstructionFuture<JSON>>> ll, boolean forceContext)
		throws ExecutionException {
		boolean isContext = forceContext;
		if (isContext == false)
			for (Pair<String, InstructionFuture<JSON>> ii : ll) {
				if ("_".equals(ii.f)) {
					isContext = true;
					break;
				}
			}
		return isContext ? new ContextObjectInstructionFuture(this, ll, builder, forceContext)
			: new ObjectInstructionFuture(this, ll, builder);
	}

	static Long longValue(JSON j) {
		Long l = null;
		switch (j.getType()) {
			case LONG:
			case DOUBLE:
			case STRING:
				return ((JSONValue) j).longValue();
		}
		return null;
	}

	static Double doubleValue(JSON j) {
		Long l = null;
		switch (j.getType()) {
			case LONG:
			case DOUBLE:
			case STRING:
				return ((JSONValue) j).doubleValue();
		}
		return null;
	}

	static String stringValue(JSON j) {
		Long l = null;
		switch (j.getType()) {
			case LONG:
			case DOUBLE:
			case STRING:
				return ((JSONValue) j).stringValue();
		}
		return null;
	}

	/*
	 * public InstructionFuture<JSON> jtl(final InstructionFuture<JSON> inst) {
	 * return new AbstractInstructionFuture() {
	 * 
	 * @Override public ListenableFuture<JSON> callItem(
	 * AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data) throws
	 * ExecutionException { return transform(inst.call(context, data), new
	 * AsyncFunction<JSON, JSON>() {
	 * 
	 * @Override public ListenableFuture<JSON> apply( JSON input) throws Exception
	 * { // sealResult(input); input.lock(); return immediateCheckedFuture(input);
	 * } }); } }; }
	 */
	/*
	 * public InstructionFuture<JSON> object(List<Pair<String,
	 * InstructionFuture<JSON>>> ll) {
	 * 
	 * try { return new InstructionFutureValue<JSON>(factory.object(ins)); } catch
	 * (ExecutionException e) { return new InstructionFutureValue<JSON>(
	 * factory.value("ExecutionException during visitObject: " +
	 * e.getLocalizedMessage())); }
	 * 
	 * 
	 * }
	 */
	public InstructionFuture<JSON> relpath(InstructionFuture<JSON> a, InstructionFuture<JSON> b) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return b.call(context, a.call(context, data));
			}

		};
	}

	public InstructionFuture<JSON> tostr(final InstructionFuture<JSON> inst) {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(inst.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return immediateCheckedFuture(builder.value(input.toString()));
					}

				});
			}
		};
	}

	public InstructionFuture<JSON> strc(final List<InstructionFuture<JSON>> ii) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				List<ListenableFuture<JSON>> rr = new ArrayList<>(ii.size());
				for (InstructionFuture<JSON> inst : ii) {
					rr.add(inst.call(context, data));
				}
				return transform(Futures.allAsList(rr), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						StringBuilder sb = new StringBuilder();
						for (JSON j : input) {
							if (j.getType() != JSONType.NULL) {
								sb.append(((JSONValue) j).stringValue());
							}
						}
						return immediateCheckedFuture(builder.value(sb.toString()));
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> abspath(InstructionFuture<JSON> inst) {
		return new AbstractInstructionFuture() {
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {

				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSON parent = input.getParent();
						while (parent != null) {
							input = parent;
							parent = input.getParent();
						}
						return inst.call(context, immediateCheckedFuture(input));
						// return immediateCheckedFuture(inst.call(context,
						// input));
					}
				});
			}
		};
	}

	public InstructionFuture<JSON> union(final List<InstructionFuture<JSON>> seq) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				final List<ListenableFuture<JSON>> fut = new ArrayList<>();
				for (InstructionFuture<JSON> ii : seq) {
					fut.add(ii.call(context, data));
				}
				return transform(Futures.allAsList(fut), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSONArray unbound = builder.array(null, true);
						for (JSON j : input) {
							unbound.add(j);
						}
						return immediateCheckedFuture(unbound);
					}

				});
			}
		};
	}

	public InstructionFuture<JSON> dereference(final InstructionFuture<JSON> a, final InstructionFuture<JSON> b) {
		return new AbstractInstructionFuture() {

			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				return transform(allAsList(a.call(context, data), b.call(context, data)),
					new AsyncFunction<List<JSON>, JSON>() {
						@Override
						public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
							Iterator<JSON> it = input.iterator();
							JSON ra = it.next();
							JSON rb = it.next();
							JSONType btype = rb.getType();
							if (btype == JSONType.NULL || btype == JSONType.OBJECT)
								return immediateCheckedFuture(builder.value());

							Frame unbound = btype == JSONType.ARRAY ? builder.frame() : null;
							switch (ra.getType()) {
								case FRAME:
								case ARRAY: {
									JSONArray larr = (JSONArray) ra;
									if (unbound != null) {
										JSONArray rarr = (JSONArray) rb;
										for (JSON j : rarr) {
											JSONType jtype = j.getType();
											if (jtype == JSONType.ARRAY) {
												JSONArray jarr = (JSONArray) j;
												Long f = longValue(jarr.get(0));
												Long s = longValue(jarr.get(1));
												int i = f < s ? 1 : -1;
												for (; f <= s; f += i) {
													JSON k = larr.get(f.intValue());
													unbound.add(k == null ? builder.value() : k.cloneJSON());
												}
											} else {
												Long l = longValue(rb);
												JSON k = larr.get(l.intValue());
												unbound.add(k == null ? builder.value() : k.cloneJSON());
											}

										}
										return immediateCheckedFuture(unbound);
									} else {
										JSON k = larr.get(((JSONValue) rb).longValue().intValue());
										return immediateCheckedFuture(k == null ? builder.value() : k.cloneJSON());
									}
								}
								case OBJECT: {
									JSONObject obj = (JSONObject) rb;
									if (unbound != null) {
										for (Pair<String, JSON> j : obj) {
											JSONType jtype = j.s.getType();
											switch (jtype) {
												case STRING:
												case LONG:
												case DOUBLE:
													String s = ((JSONValue) rb).stringValue();
													unbound.add(obj.get(s));
												default:
													unbound.add(builder.value());
											}
										}
										return immediateCheckedFuture(unbound);
									} else {
										return immediateCheckedFuture(builder.value());
									}
								}
								default:
									return immediateCheckedFuture(builder.value());
							}
						}
					});
			}
		};
	}

	public InstructionFuture<JSON> addInstruction(InstructionFuture<JSON> a, InstructionFuture<JSON> b) {
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
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONArray r) {
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
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
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
			public JSONObject op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONObject r) {
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

	public InstructionFuture<JSON> subInstruction(InstructionFuture<JSON> a, InstructionFuture<JSON> b) {
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
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONArray r) {
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
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
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
			public JSONObject op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONObject r) {
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

	public InstructionFuture<JSON> mulInstruction(InstructionFuture<JSON> a, InstructionFuture<JSON> b) {
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

	public InstructionFuture<JSON> divInstruction(InstructionFuture<JSON> a, InstructionFuture<JSON> b) {
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

	public InstructionFuture<JSON> modInstruction(InstructionFuture<JSON> a, InstructionFuture<JSON> b) {
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
