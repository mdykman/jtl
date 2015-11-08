package org.dykman.jtl.future;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedCheckedFuture;
//import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.jsonVisitor;
import org.dykman.jtl.future.ObjectInstructionBase.ObjectKey;
import org.dykman.jtl.json.JList;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONContainer;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;
import org.dykman.jtl.modules.ModuleLoader;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class FutureInstructionFactory {

	protected static final String JTL_INTERNAL = "_jtl_internal_";
	protected static final Method stringFormatter;

	static {
		try {
			stringFormatter = Formatter.class.getMethod("format", String.class, Object[].class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ExceptionInInitializerError("unable to load formatter");
		}
	}
	// rank all

	public static FutureInstruction<JSON> memo(SourceInfo meta, final FutureInstruction<JSON> inst) {
		if (inst instanceof MemoInstructionFuture)
			return inst;
		meta.name = "memo";
		return new MemoInstructionFuture(meta, inst);
	}

	/*
	 * public JSONBuilder builder() { return builder; }
	 */
	// rank: all

	public static FutureInstruction<JSON> file(SourceInfo meta) {
		meta.name = "file";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				FutureInstruction<JSON> f = context.getdef("1");
				return transform(f.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						final FutureInstruction<JSON> dd = context.getdef("2");
						final File ff = context.file(stringValue(input));
						if (dd == null) {
							Callable<JSON> cc = new Callable<JSON>() {
								@Override
								public JSON call() throws Exception {
									if (ff.exists())
										return context.builder().parse(ff);
									logger.error("in file(): failed to find file " + ff.getPath());
									return JSONBuilderImpl.NULL;
								}
							};
							return context.executor().submit(cc);
						} else {
							return transform(dd.call(context, data), new AsyncFunction<JSON, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(final JSON dataout) throws Exception {
									Callable<JSON> cc = new Callable<JSON>() {
										@Override
										public JSON call() throws Exception {
											Writer out = new FileWriter(ff);
											dataout.write(out, 2, true);
											out.flush();
											return context.builder().value(0L);
										}
									};
									return context.executor().submit(cc);
								}
							});
						}
					}
				});
			}
		};
	}

	static Random random = new Random();

	public static FutureInstruction<JSON> rand(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				FutureInstruction<JSON> f = context.getdef("1");
				FutureInstruction<JSON> s = context.getdef("2");
				ArrayList<ListenableFuture<JSON>> d = new ArrayList<>();
				// d.add(data);
				if (f != null) {
					d.add(f.call(context, data));
				}
				if (s != null) {
					d.add(s.call(context, data));
				}
				return transform(allAsList(d), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						// JSON parent = jit.next();
						int n = input.size();
						JSONBuilder builder = context.builder();
						if (n == 0) {
							// no params
							return immediateCheckedFuture(builder.value(random.nextDouble()));
						}
						JSON jf = jit.next();
						if (jf instanceof JSONArray) {
							JSONArray ja = (JSONArray) jf;
							JSON jj = ja.get(random.nextInt(ja.size()));
							return immediateCheckedFuture(jj);
						}

						Number nf = (Number) ((JSONValue) jf).get();
						if (n == 1) {
							return nf.intValue() == 0 ? immediateCheckedFuture(builder.value(random.nextDouble()))
									: immediateCheckedFuture(builder.value(new Long(random.nextInt(nf.intValue()))));
						}
						// 2 params
						JSON js = jit.next();
						if (js == null) {
							return immediateCheckedFuture(JSONBuilderImpl.NULL);
						}
						int cc = nf.intValue();
						switch (js.getType()) {
						case LIST:
						case ARRAY: {
							JSONArray ja = (JSONArray) js;
							JSONArray jout = context.builder().array(null);
							for (int i = 0; i < cc; ++i) {
								JSON jj = ja.get(random.nextInt(ja.size()));
								jout.add(jj);
							}
							return immediateCheckedFuture(jout);
						}
						case LONG:
						case DOUBLE: {
							Number nn = (Number) ((JSONValue) js).get();
							JSONArray jout = context.builder().array(null);
							for (int i = 0; i < cc; ++i) {
								JSON jj = builder.value(random.nextInt(nn.intValue()));
								jout.add(jj);
							}
							return immediateCheckedFuture(jout);
						}
						default:
							throw new ExecutionException("bad second parameter to rand(): " + js.toString(false),
									source);
						}
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> url(SourceInfo meta) {
		meta.name = "url";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> f = context.getdef("1");
				return transform(f.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						Callable<JSON> cc = new Callable<JSON>() {

							@Override
							public JSON call() throws Exception {
								URL url = new URL(stringValue(input));
								InputStream in = url.openStream();
								System.err.println("opened url " + url.toExternalForm());
								return context.builder().parse(in);
							}
						};
						return context.executor().submit(cc);
					}
				});
			}
		};
	}

	// rank: all
	public static FutureInstruction<JSON> map(SourceInfo meta) {
		meta.name = "map";

		return new AbstractFutureInstruction(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				FutureInstruction<JSON> gbe = context.getdef("1");

				final FutureInstruction<JSON> mapfunc;
				if (gbe != null) {
					mapfunc = gbe.unwrap();
				} else {
					return immediateCheckedFuture(JSONBuilderImpl.NULL);
				}
				AsyncFunction<JSON, JSON> asy = new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(final JSON input) throws Exception {
						switch (input.getType()) {
						case OBJECT: {
							JSONObject obj = (JSONObject) input;
							List<ListenableFuture<Pair<String, JSON>>> ll = new ArrayList<>();

							for (Pair<String, JSON> pp : obj) {
								AsyncExecutionContext<JSON> ctx = context.createChild(false, false, data, meta);
								ctx.define("0", value(context.builder().value("map"), getSourceInfo()));
								FutureInstruction<JSON> ki = value(context.builder().value(pp.f), getSourceInfo());
								// ctx.define(JTL_INTERNAL_KEY, ki);
								ctx.define("key", ki);
								ctx.define(":", ki);
								final String kk = pp.f;
								ListenableFuture<JSON> remapped = mapfunc.call(context, immediateCheckedFuture(pp.s));
								ll.add(transform(remapped,
										new KeyedAsyncFunction<JSON, Pair<String, JSON>, String>(kk) {
									@Override
									public ListenableFuture<Pair<String, JSON>> apply(JSON input) throws Exception {
										return immediateCheckedFuture(new Pair<>(k, input));
									}
								}));

							}
							return transform(allAsList(ll), new AsyncFunction<List<Pair<String, JSON>>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<Pair<String, JSON>> input2) throws Exception {
									JSONObject result = context.builder().object(null);
									for (Pair<String, JSON> pp : input2) {
										result.put(pp.f, pp.s);
									}

									return immediateCheckedFuture(result);
								}
							});
						}
						case ARRAY: {
							List<ListenableFuture<JSON>> ll = new ArrayList<>();
							for (JSON jj : (JSONArray) input) {
								ll.add(mapfunc.call(context, immediateCheckedFuture(jj)));
							}
							return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<JSON> linput) throws Exception {
									JSONArray array = context.builder().array(input.getParent());
									for (JSON j : linput) {
										array.add(j);
									}
									return immediateCheckedFuture(array);
								}
							});
						}

						default:
							return data;
						}
					}
				};
				return transform(data, asy);
			}
		};
	}

	// rank: all
	public static FutureInstruction<JSON> groupBy(SourceInfo meta) {
		meta.name = "group";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> gbe = context.getdef("1");
				if (gbe != null) {
					gbe = gbe.unwrap();
				} else {
					return data;
				}

				final FutureInstruction<JSON> filter = gbe;
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						// JSONObject obj = builder.object(null);
						JSONType type = input.getType();
						if (type != JSONType.ARRAY && type != JSONType.LIST)
							return immediateCheckedFuture(context.builder().object(null));
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
								JSONObject obj = context.builder().object(null);
								for (Pair<JSON, JSON> pp : input) {
									String s = stringValue(pp.s);
									JSONArray a = (JSONArray) obj.get(s);
									if (a == null) {
										a = context.builder().array(obj);
										obj.put(s, a, true);
									}
									a.add(pp.f);
								}
								return immediateCheckedFuture(obj);
								// TODO Auto-generated method stub
								// return null;
							}

						});
					}
				});
			}
		};
	}

	// rank: all, probably
	public static FutureInstruction<JSON> importInstruction(SourceInfo meta, final JSON conf) {
		meta.name = "import";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>(8);
				FutureInstruction<JSON> key = context.getdef(":");
				final SourceInfo si = this.source;
				ll.add(key.call(context, data));
				int cc = 1;
				while (true) {
					FutureInstruction<JSON> files = context.getdef(Integer.toString(cc++));
					if (files == null)
						break;
					ll.add(files.call(context, data));
				}

				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						final String key = stringValue(jit.next());
						final List<FutureInstruction<JSON>> imperitives = new ArrayList<>(input.size());

						AsyncExecutionContext<JSON> mc = context.getInit().getNamedContext(key, true, false, source);

						FutureInstruction<JSON> initInst = null;
						while (jit.hasNext()) {
							String ff = stringValue(jit.next());
							File toParse = new File(mc.currentDirectory(), ff);
							FutureInstruction<JSON> inst = mc.compiler().parse(toParse);
							if (inst instanceof FixedContext) {
								FixedContext fi = (FixedContext) inst;
								inst = fi.getIntruction();
								if (inst instanceof ObjectInstructionBase) {
									ObjectInstructionBase oib = (ObjectInstructionBase) inst;
									List<Pair<ObjectKey, FutureInstruction<JSON>>> prs = oib.pairs();
									for (Pair<ObjectKey, FutureInstruction<JSON>> ip : prs) {
										String k = ip.f.label;
										boolean notquoted = !ip.f.quoted;
										FutureInstruction<JSON> fii = fixContextData(ip.s);
										if (notquoted && "!init".equals(k)) {
											initInst = FutureInstructionFactory.memo(ip.s.getSourceInfo(), fii);
											mc.define(k.substring(1), initInst);
										} else if (notquoted && "_".equals(k)) {
											context.getInit().define(key, fii);
										} else {
											char ic = k.charAt(0);
											if (notquoted && ic == '$') {
												mc.define(k.substring(1), FutureInstructionFactory
														.deferred(fii.getSourceInfo(), fii, context, data));
											} else if (notquoted && ic == '!') {
												FutureInstruction<JSON> imp = FutureInstructionFactory
														.memo(fii.getSourceInfo(), fii);
												mc.define(k.substring(1), imp);
												imperitives.add(imp);
											} else {
												mc.define(k, fii);
											}
										}
									}
								}
							}
						}
						return ObjectInstructionBase.runImperatives(initInst, imperitives, mc, data);
						// throw new ExecutionException("imported file did not
						// represent an object",si);
					}
				});
			}

		};
	}

	// rank all
	public static FutureInstruction<JSON> loadModule(SourceInfo meta, final JSONObject conf) {
		meta.name = "loadmodule";
		JSONObject modconf = (JSONObject) conf.get("modules");
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>(12);
				FutureInstruction<JSON> key = context.getdef(":");
				FutureInstruction<JSON> name = context.getdef("1");
				ll.add(key.call(context, data));
				ll.add(name.call(context, data));

				FutureInstruction<JSON> ci = context.getdef("2");
				if (ci != null) {
					ll.add(ci.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						String key = stringValue(jit.next());
						String name = stringValue(jit.next());
						JSONObject config = (JSONObject) (jit.hasNext() ? jit.next() : context.builder().object(null));

						JSON smode = conf.get("server-mode");

						ModuleLoader ml = ModuleLoader.getInstance(context.currentDirectory(), context.builder(), conf);
						AsyncExecutionContext<JSON> modctx = context.getInit().getNamedContext(key, true, false, meta);
						JSON n = ml.create(meta, name, key, modctx, smode == null ? false : smode.isTrue(), config);
						return immediateCheckedFuture(n);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> reMatch(SourceInfo meta, final String p, final FutureInstruction<JSON> d) {
		meta.name = "rematch";
		final Pattern pattern = Pattern.compile(p);
		return new AbstractFutureInstruction(meta, true) {

			private JSON applyRegex(Pattern p, JSON j, AsyncExecutionContext<JSON> context) {
				switch (j.getType()) {
				case STRING:
					String ins = ((JSONValue) j).stringValue();
					if (ins != null) {
						Matcher m = p.matcher(ins);
						if (m.find()) {
							JSONArray unbound = context.builder().array(null);
							int n = m.groupCount();
							for (int i = 0; i <= n; ++i) {
								JSON r = context.builder().value(m.group(i));
								unbound.add(r);
							}
							return unbound;
						}
					}
					break;
				case OBJECT: {
					JSONObject unbound = context.builder().object(null);
					JSONObject inarr = (JSONObject) j;
					for (Pair<String, JSON> jj : inarr) {
						JSON r = applyRegex(p, jj.s, context);
						if (r.isTrue())
							unbound.put(jj.f, r);
					}
					return unbound;
				}
				case LIST:
				case ARRAY: {
					JSONArray unbound = context.builder().array(null);
					JSONArray inarr = (JSONArray) j;
					for (JSON k : inarr) {
						JSON r = applyRegex(p, k, context);
						if (r.isTrue())
							unbound.add(r);
					}
					return unbound;
				}
				default:
				}
				return context.builder().array(null);
			}

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(d.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						// frame.add(applyRegex(pattern, input));
						return immediateCheckedFuture(applyRegex(pattern, input, context));
					}
				});
			}
		};
	}

	// rank: item
	public static FutureInstruction<JSON> negate(SourceInfo meta, FutureInstruction<JSON> ii) {
		meta.name = "negate";
		// TODO:: need to actually apply negation here: boring but eventually
		// necessary
		return ii;
	}

	// rank: all
	public static FutureInstruction<JSON> deferred(SourceInfo meta, FutureInstruction<JSON> inst,
			AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t) {
		SourceInfo si = inst.getSourceInfo().clone();
		si.name = "deferred";
		return memo(meta, new DeferredCall(si, inst, context, t));
	}

	public static FutureInstruction<JSON> thread(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final FutureInstruction<JSON> arg = context.getdef("1");
				if (arg == null)
					return data;
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						Callable<JSON> callable = new Callable<JSON>() {
							@Override
							public JSON call() throws Exception {
								return arg.call(context, Futures.immediateCheckedFuture(input)).get();
							}
						};
						return context.executor().submit(callable);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> sum(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null) {
					data = arg.call(context, data);
				}

				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input instanceof JSONArray) {
							Long acc = 0L;
							Double facc = 0.0;
							boolean islong = true;
							for (JSON j : (JSONArray) input) {
								if (j.isNumber()) {
									Number n = (Number) ((JSONValue) j).get();
									if (islong) {
										if (n instanceof Double) {
											facc = acc.doubleValue();
											facc += n.doubleValue();
											islong = false;
										} else {
											acc += n.longValue();
										}
									} else {
										facc += n.doubleValue();
									}
								}
							}
							return islong ? immediateCheckedFuture(context.builder().value(acc))
									: immediateCheckedFuture(context.builder().value(facc));
						}
						return Futures.immediateCheckedFuture(JSONBuilderImpl.NULL);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> avg(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null) {
					data = arg.call(context, data);
				}
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input instanceof JSONArray) {
							int cc = 0;
							Double facc = 0.0;
							for (JSON j : (JSONArray) input) {
								if (j.isNumber()) {

									Number n = (Number) ((JSONValue) j).get();
									Double d = n.doubleValue();

									double inc = (d - facc) / (cc++);
									facc += inc;
								}
							}
							return immediateCheckedFuture(context.builder().value(facc));
						}
						return Futures.immediateCheckedFuture(JSONBuilderImpl.NULL);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> min(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null) {
					data = arg.call(context, data);
				}
				// final ListenableFuture<JSON> d=data;
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input instanceof JSONArray) {
							JSONArray ina = (JSONArray) input;
							if (ina.size() == 0)
								Futures.immediateCheckedFuture(context.builder().value(0L));
							Long acc = null;
							Double facc = null;
							boolean islong = true;
							for (JSON j : ina) {
								if (j.isNumber()) {
									Number n = (Number) ((JSONValue) j).get();
									if (acc == null)
										acc = n.longValue();
									if (facc == null)
										facc = n.doubleValue();
									if (islong) {
										if (n instanceof Double) {
											facc = acc.doubleValue();
											Double d = n.doubleValue();
											facc = facc < d ? facc : d;
											islong = false;
										} else {
											Long l = n.longValue();
											acc = acc < l ? acc : l;
										}
									} else {
										Double d = n.doubleValue();
										facc = facc < d ? facc : d;
									}
								}
							}
							return islong ? immediateCheckedFuture(context.builder().value(acc))
									: immediateCheckedFuture(context.builder().value(facc));
						}
						return Futures.immediateCheckedFuture(JSONBuilderImpl.NULL);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> max(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null) {
					data = arg.call(context, data);
				}
				// final ListenableFuture<JSON> d=data;
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input instanceof JSONArray) {
							JSONArray ina = (JSONArray) input;
							if (ina.size() == 0)
								Futures.immediateCheckedFuture(context.builder().value(0L));
							Long acc = null;
							Double facc = null;
							boolean islong = true;
							for (JSON j : ina) {
								if (j.isNumber()) {
									Number n = (Number) ((JSONValue) j).get();
									if (acc == null)
										acc = n.longValue();
									if (facc == null)
										facc = n.doubleValue();
									if (islong) {
										if (n instanceof Double) {
											facc = acc.doubleValue();
											Double d = n.doubleValue();
											facc = facc > d ? facc : d;
											islong = false;
										} else {
											Long l = n.longValue();
											acc = acc > l ? acc : l;
										}
									} else {
										Double d = n.doubleValue();
										facc = facc > d ? facc : d;
									}
								}
							}
							return islong ? immediateCheckedFuture(context.builder().value(acc))
									: immediateCheckedFuture(context.builder().value(facc));
						}
						return Futures.immediateCheckedFuture(JSONBuilderImpl.NULL);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> append(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				int cc = 1;
				FutureInstruction<JSON> inst = null;
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				while (null != (inst = context.getdef(Integer.toString(cc)))) {
					ll.add(inst.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON dd = jit.next();
						if (dd instanceof JSONArray) {
							JSONArray arr = context.builder().array(dd.getParent());
							for (JSON jj : (JSONArray) dd) {
								arr.add(jj);
							}
							while (jit.hasNext()) {
								JSON jj = jit.next();
								if (jj instanceof JList) {
									for (JSON jjj : (JList) jj) {
										arr.add(jjj);
									}
								} else {
									arr.add(jj);
								}
							}
							return immediateCheckedFuture(context.builder().value(arr));
						}
						return immediateCheckedFuture(dd);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> defined(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final FutureInstruction<JSON> inst = context.getdef("1");
				return transform(inst.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						String k = stringValue(input);
						return immediateCheckedFuture(context.builder().value(context.getdef(k) != null));
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> each(SourceInfo meta) {
		return new AbstractFutureInstruction(meta, false) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final FutureInstruction<JSON> inst = context.getdef("1").unwrap();
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						// return inst.call(context,
						// immediateCheckedFuture(input));

						if (input instanceof JSONArray) {
							final boolean isList = input instanceof JList;
							List<ListenableFuture<JSON>> ll = new ArrayList<>();
							for (JSON j : (JSONArray) input) {
								ll.add(inst.call(context, immediateCheckedFuture(j)));
							}
							return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
									JSONArray f = isList ?  
										context.builder().list() :  
										context.builder().array(null);
									for (JSON j : input) {
										f.add(j);
									}
									return immediateCheckedFuture(f);
								}
							});
						} else {
							return inst.call(context, immediateCheckedFuture(input));
						}

					}

				});
			}
		};
	}

	public static FutureInstruction<JSON> variable(SourceInfo meta, final String name) {
		final FunctionInvocationInstruction fi = function(meta, name, null);
		fi.setVariable(true);
		return new AbstractFutureInstruction(fi.getSourceInfo()) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				try {
					return fi.call(context, data);
				} catch (ExecutionException e) {
					// eat the access exception and quietly return null
					return immediateCheckedFuture(JSONBuilderImpl.NULL);
				}
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> activate(SourceInfo meta, final String name,
			final List<FutureInstruction<JSON>> iargs) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {

				FutureInstruction<JSON> inst = context.getdef(name);
				if (inst == null) {
					throw new ExecutionException(source);
				}
				// inst = inst.unwrap(context);
				// if(inst instanceof DeferredCall) {
				inst = deref(context, data, inst, iargs);

				// }
				return inst.call(context, data);
			}

			public FutureInstruction<JSON> deref(AsyncExecutionContext<JSON> ctx, ListenableFuture<JSON> data,
					FutureInstruction<JSON> inst, List<FutureInstruction<JSON>> ll) {
				AsyncExecutionContext<JSON> cc = ctx.createChild(true, false, data, source);
				int ctr = 1;
				for (FutureInstruction<JSON> i : ll) {
					FutureInstruction<JSON> ins = wrapArgument(source, ctx, i, data);
					cc.define(Integer.toString(ctr++), ins);
				}

				DeferredCall dc = new DeferredCall(source, inst.getBareInstruction(), cc, null);
				return dc;

			}

		};
	}

	public static FutureInstruction<JSON> paramArray(SourceInfo meta, final List<FutureInstruction<JSON>> iargs) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				for (FutureInstruction<JSON> ii : iargs) {
					ll.add(ii.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSONArray arr = context.builder().array(null);
						for (JSON j : input) {
							arr.add(j);
						}
						return immediateCheckedFuture(arr);
					}
				});
			}
		};
	}

	static FutureInstruction<JSON> wrapArgument(SourceInfo info, AsyncExecutionContext<JSON> ctx,
			FutureInstruction<JSON> inst, final ListenableFuture<JSON> data) {
		return memo(info, deferred(info, inst, ctx.declaringContext(), data));

	}

	public static FunctionInvocationInstruction function(SourceInfo meta, final String name,
			final List<FutureInstruction<JSON>> iargs) {
		meta.name = "function";
		return new FunctionInvocationInstruction(meta, name, iargs);
	}

	// rank all
	public static FutureInstruction<JSON> value(final ListenableFuture<JSON> o, SourceInfo meta) {
		meta.name = "value";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return o;
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> value(JSON o, SourceInfo meta) {
		return value(immediateCheckedFuture(o), meta);
	}

	// rank all
	public static FutureInstruction<JSON> value(JSONBuilder builder, SourceInfo meta) {
		return value(JSONBuilderImpl.NULL, meta);
	}

	// rank all
	public static FutureInstruction<JSON> value(final Boolean val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static FutureInstruction<JSON> value(final Long val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static FutureInstruction<JSON> value(final Double val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static FutureInstruction<JSON> value(final String val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static FutureInstruction<JSON> number(Number num, JSONBuilder builder, SourceInfo meta) {
		meta.name = "number";
		return value(builder.value(num), meta);
	}

	public static FutureInstruction<JSON> listToArray(final FutureInstruction<JSON> inst, SourceInfo meta) {
		meta.name = "l2f";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) throws ExecutionException {
				return transform(inst.call(context, t), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input.getType() == JSONType.LIST) {
							JList f = (JList) input;
							return immediateCheckedFuture(context.builder().array(input.getParent(), f.collection()));
						} else {
							return immediateCheckedFuture(input);
						}
					}
				});
			}
		};
	}

	// rank all
	static ListenableFuture<List<JSON>> enlist(ListenableFuture<JSON> a) {
		return transform(a, new AsyncFunction<JSON, List<JSON>>() {

			@Override
			public ListenableFuture<List<JSON>> apply(JSON input) throws Exception {
				List<JSON> il = new ArrayList<>();
				il.add(input);
				return immediateCheckedFuture(il);
			}
		});
	}

	public static FutureInstruction<JSON> array(final List<FutureInstruction<JSON>> ch, SourceInfo meta) {
		meta.name = "array";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) throws ExecutionException {
				List<ListenableFuture<JSON>> args = new ArrayList<>();
				final Iterator<FutureInstruction<JSON>> fit = ch.iterator();
				while (fit.hasNext()) {
					FutureInstruction<JSON> fi = fit.next();
					if (fi instanceof RangeInstruction) {
						RangeInstruction rri = (RangeInstruction) fi;
						args.add(rri.call(context, t));
					} else {
						ListenableFuture<JSON> fa = fi.call(context, t);
						args.add(transform(fi.call(context, t), new AsyncFunction<JSON, JSON>() {
							@Override
							public ListenableFuture<JSON> apply(JSON input) throws Exception {
								JSONArray a = context.builder().array(null);
								a.add(input);
								return immediateCheckedFuture(a);
							}
						}));
					}
				}
				return transform(allAsList(args), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSONArray arr = context.builder().array(null, input.size());
						for (JSON jarr : input) {
							for (JSON j : (JSONArray) jarr) {
								if (j.getType() == JSONType.LIST) {
									JList jl = (JList) j;
									for (JSON jj : jl) {
										arr.add(jj == null ? JSONBuilderImpl.NULL : jj);
									}
								} else {
									arr.add(j == null ? JSONBuilderImpl.NULL : j);
								}
							}
						}
						return immediateCheckedFuture(arr);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> dyadic(SourceInfo meta, FutureInstruction<JSON> left,
			FutureInstruction<JSON> right, DyadicAsyncFunction<JSON> f) {
		return dyadic(meta, left, right, f, true);
	}

	public static FutureInstruction<JSON> dyadic(SourceInfo meta, FutureInstruction<JSON> left,
			FutureInstruction<JSON> right, DyadicAsyncFunction<JSON> f, boolean items) {
		return new AbstractFutureInstruction(meta, items) {

			@SuppressWarnings("unchecked")
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
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
							return immediateFailedCheckedFuture(e);
						}
					}
				});
			}
		};

	}

	public static FutureInstruction<JSON> errorHandler(
			final AsyncExecutionContext<JSON> context,
			final FutureInstruction<JSON> inst,
			final FutureInstruction<JSON> handler,
			final SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {
			

			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				try {
					final ListenableFuture<JSON> result = inst.call(context, data);
					return new ForwardingListenableFuture<JSON>() {
						@Override
						public JSON get() {
							try {
								return delegate().get();
							} catch (Exception e) {
								try {
									return handleException(context,e).get();
								} catch (java.util.concurrent.ExecutionException|InterruptedException e1) {
									throw new RuntimeException("catastrophic error while error handling", e1);
								}
							}
						}
						@Override
						public JSON get(long timeout,TimeUnit unit) {
							try {
								return delegate().get(timeout,unit);
							} catch (Exception e) {
								try {
									return handleException(context, e).get();
								} catch (java.util.concurrent.ExecutionException|InterruptedException e1) {
									throw new RuntimeException("catastrophic error while error handling", e1);
								}
							}
						}

						@Override
						protected ListenableFuture<JSON> delegate() {
							return result;
						}
					};
				} catch (Exception e) {
					return handleException(context, e);
				}
			}

			protected ListenableFuture<JSON> evaluateError(AsyncExecutionContext<JSON> context, SourceInfo si,
					Throwable e) throws ExecutionException {
				JSONBuilder builder = context.builder();
				List<FutureInstruction<JSON>> iargs = new ArrayList<>(2);
				iargs.add(FutureInstructionFactory.value(
						builder.value(e.getLocalizedMessage()), source));
				FutureInstruction<JSON> eh = new FunctionInvocationInstruction(
						source, "error", iargs, handler);

				return eh.call(context, immediateCheckedFuture(si.toJson(builder)));
			}
			
			protected ListenableFuture<JSON> handleException(
					AsyncExecutionContext<JSON> ctx,Exception e) {
				ctx.exception(e);
				Throwable t = e;
				SourceInfo si;

				if(!(t instanceof java.util.concurrent.ExecutionException)) {
					t = t.getCause();
				}

				// TODO:: this exception digging could be more robust
				if(!(t instanceof ExecutionException)) {
					t = t.getCause();
				}
				if (t instanceof ExecutionException) {
					ExecutionException ee = (ExecutionException) t;
					si = ee.getSourceInfo();
				} else {
					si = SourceInfo.internal("internal");
				}
				try {
					return evaluateError(context, si, t);
				} catch (Exception ee) {
					throw new RuntimeException("error handler failed catastrophically", ee);
				}
			}
		};
	}

	public static final ListenableFuture<JSON> FNULL = immediateCheckedFuture(JSONBuilderImpl.NULL);

	// rank all
	public static FutureInstruction<JSON> pivot(SourceInfo meta) {
		meta.name = "pivot";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input == null || !(input instanceof JSONArray)) {
							return FNULL;
						}
						JSONArray outer = context.builder().array(null);
						boolean first = true;
						for (JSON j : (JSONArray) input) {
							if (j == null || !(j instanceof JSONArray)) {
								return FNULL;
							}
							int coli = 0;
							for (JSON jj : (JSONArray) j) {
								JSONArray inner;
								if (first) {
									inner = context.builder().array(null);
								} else {
									inner = (JSONArray) outer.get(coli);
								}
								inner.add(jj);
								if (first)
									outer.add(inner);
								coli++;
							}
							first = false;
						}
						return immediateCheckedFuture(outer);
					}
				});
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> unique(SourceInfo meta) {
		meta.name = "unique";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						switch (input.getType()) {
						case LIST:
						case ARRAY: {
							Collection<JSON> cc = ((JSONArray) input).collection();
							Set<JSON> ss = new LinkedHashSet<>();
							for (JSON j : cc) {
								ss.add(j);
							}
							return immediateCheckedFuture(context.builder().array((JSON) input, ss));

						}
						default:
							return immediateCheckedFuture(input);
						}
					}

				});
			}

		};
	}

	public static FutureInstruction<JSON> substr(SourceInfo meta) {
		meta.name = "substr";

		return new AbstractFutureInstruction(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final FutureInstruction<JSON> exp1 = context.getdef("1");
				if (exp1 == null) {
					return data;
				}
				final FutureInstruction<JSON> exp2 = context.getdef("2");
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				ll.add(exp1.call(context, data));
				if (exp2 != null)
					ll.add(exp2.call(context, data));
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON str = jit.next();
						JSON p1 = jit.next();
						JSON p2 = jit.hasNext() ? jit.next() : null;
						if (p1.isNumber() && (p2 == null || p2.isNumber())) {
							if (str.getType() == JSONType.STRING) {
								String s = stringValue(str);
								if (p2 != null) {
									s = s.substring(longValue(p1).intValue(), longValue(p2).intValue());
								} else {
									s = s.substring(longValue(p1).intValue());
								}
								return immediateCheckedFuture(context.builder().value(s));
							}
						}
						return immediateCheckedFuture(str);
					}
				});

			}
		};
	}

	public static FutureInstruction<JSON> split(SourceInfo meta) {
		meta.name = "split";

		return new AbstractFutureInstruction(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> exp = context.getdef("1");
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				if (exp == null) {
					ll.add(immediateCheckedFuture(context.builder().value("\\s+")));
				} else {
					ll.add(exp.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON d = jit.next();
						JSON a = jit.next();
						if (a.isValue() && d.isValue()) {
							JSONBuilder builder = context.builder();
							String ds = stringValue(d);
							String as = stringValue(a);
							String[] sas = ds.split(as);
							JSONArray arr = builder.array(a.getParent());
							for (String s : sas) {
								arr.add(builder.value(s));
							}
							return immediateCheckedFuture(arr);
						}
						return immediateCheckedFuture(d);
					}
				});

			}
		};
	}

	public static FutureInstruction<JSON> write(SourceInfo meta) {
		meta.name = "write";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final FutureInstruction<JSON> arg = context.getdef("1");
				if (arg == null)
					return immediateFailedCheckedFuture(
							new ExecutionException("write() requires a filename argument", meta));
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				ll.add(arg.call(context, data));
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						final JSON d = jit.next();
						JSON a = jit.next();
						final String l = stringValue(a);
						Callable<JSON> cc = new Callable<JSON>() {

							@Override
							public JSON call() throws Exception {
								FileWriter fw = new FileWriter(l);
								d.write(fw, 3, true);
								fw.flush();
								fw.close();
								return context.builder().value(0);
							}
						};
						return context.executor().submit(cc);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> copy(SourceInfo meta) {
		meta.name = "copy";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final FutureInstruction<JSON> arg = context.getdef("1");
				if (arg == null)
					return immediateFailedCheckedFuture(
							new ExecutionException("copy() requires a numeric argument", meta));
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				ll.add(arg.call(context, data));
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON d = jit.next();
						JSON a = jit.next();
						Long l = longValue(a);
						JSONArray arr = context.builder().array(d.getParent());
						if (l != null) {
							for (int i = 0; i < l.intValue(); ++i) {
								arr.add(d);
							}
							return immediateCheckedFuture(arr);
						}
						return immediateCheckedFuture(d);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> join(SourceInfo meta) {
		meta.name = "join";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final FutureInstruction<JSON> arg = context.getdef("1");
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				if (arg == null) {
					ll.add(immediateCheckedFuture(context.builder().value("")));
				} else {
					ll.add(arg.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON d = jit.next();
						JSON a = jit.next();
						if (a.isValue()) {
							String sep = stringValue(a);
							JSONType type = d.getType();
							if (type == JSONType.ARRAY || type == JSONType.LIST) {
								StringBuilder sb = new StringBuilder();
								boolean first = true;
								for (JSON j : (JSONArray) d) {
									if (!first)
										sb.append(sep);
									else {
										first = false;
									}
									sb.append(stringValue(j));
								}
								return immediateCheckedFuture(context.builder().value(sb.toString()));
							}
						}
						return immediateCheckedFuture(d);
					}
				});
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> count(SourceInfo meta) {
		meta.name = "count";
		return new AbstractFutureInstruction(meta) {
			JSON cnt(JSON j, JSONBuilder builder) {
				switch (j.getType()) {
				case LIST:
					/*
					 * { Frame f = builder.frame(); for (JSON jj : (Frame) j) {
					 * f.add(cnt(jj)); } return f; }
					 */
				case ARRAY:
					return builder.value(((JSONArray) j).collection().size());
				case OBJECT:
					return builder.value(((JSONObject) j).map().size());
				case STRING:
					return builder.value(((JSONValue) j).stringValue().length());
				default:
					return builder.value(1);
				}

			}

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null) {
					data = arg.call(context, data);
				}
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return immediateCheckedFuture(cnt(input, context.builder()));
					}
				});
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> conditional(final FutureInstruction<JSON> test,
			final FutureInstruction<JSON> trueI, final FutureInstruction<JSON> falseI, SourceInfo meta) {
		meta.name = "conditional";
		return new AbstractFutureInstruction(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				return transform(test.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input.isTrue()) {
							return trueI.call(context, data);
						} else {
							return falseI.call(context, data);
						}
					}
				});
			}
		};
	}

	// rank all
	@SafeVarargs
	public static FutureInstruction<JSON> chain(SourceInfo meta, final FutureInstruction<JSON>... inst) {
		meta.name = "chain";
		FutureInstruction<JSON> chain = null;
		for (FutureInstruction<JSON> ii : inst) {
			if (chain == null) {
				chain = ii;
			} else {
				final FutureInstruction<JSON> pp = chain;
				chain = new AbstractFutureInstruction(meta) {

					@Override
					public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context,
							ListenableFuture<JSON> data) throws ExecutionException {
						return ii.call(context, pp.call(context, data));
					}
				};
			}
		}
		return chain;
	}

	// rank all
	@SafeVarargs
	public static FutureInstruction<JSON> sequence(SourceInfo meta, final FutureInstruction<JSON>... inst) {
		meta.name = "sequence";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				for (FutureInstruction<JSON> ii : inst) {
					ll.add(ii.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON p = jit.next();
						JSONArray f = context.builder().array(p);
						while (jit.hasNext()) {
							f.add(jit.next());
						}
						return immediateCheckedFuture(f);
					}
				});
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> params(SourceInfo meta) {
		meta.name = "params";
		return new AbstractFutureInstruction(meta) {

			@SuppressWarnings("unchecked")
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				boolean done = false;

				List<FutureInstruction<JSON>> ll = new ArrayList<>();
				for (int i = 1; done == false && i < 4096; ++i) {
					FutureInstruction<JSON> ci = context.getdef(Integer.toString(i));
					if (ci == null)
						done = true;
					else {
						ll.add(ci);
					}
				}
				return sequence(meta, (FutureInstruction<JSON>[]) ll.toArray(new FutureInstruction[ll.size()]))
						.call(context, data);
			}

		};

	}

	public static FutureInstruction<JSON> switchInst(SourceInfo meta) {
		meta.name = "switch";
		return new AbstractFutureInstruction(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final FutureInstruction<JSON> f = context.getdef("1");
				final FutureInstruction<JSON> s = context.getdef("2");
				if (f == null || s == null)
					return immediateFailedCheckedFuture(new ExecutionException("switch missing parameters", source));
				FutureInstruction<JSON> body = s.getBareInstruction();
				if (body instanceof ObjectInstructionBase) {
					final ObjectInstructionBase base = (ObjectInstructionBase) body;
					final AsyncExecutionContext<JSON> pcontext = context.getParent();
					return transform(f.call(pcontext, data), new AsyncFunction<JSON, JSON>() {
						@Override
						public ListenableFuture<JSON> apply(JSON input) throws Exception {
							String str = stringValue(input);
							FutureInstruction<JSON> defi = null;
							for (Pair<ObjectKey, FutureInstruction<JSON>> pp : base.pairs()) {

								if ("_".equals(pp.f.label) && pp.f.quoted) {
									defi = pp.s;
								}
								if (str.equals(pp.f)) {
									return pp.s.call(pcontext, data);
								}
							}
							if (defi != null) {
								return defi.call(pcontext, data);
							}
							return immediateCheckedFuture(JSONBuilderImpl.NULL);
						}
					});
				} else
					return immediateFailedCheckedFuture(
							new ExecutionException("switch expects an object as a second parameter", meta));
			}

		};
	}

	public static FutureInstruction<JSON> trace(SourceInfo meta) {
		meta.name = "trace";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				int cc = 1;
				final SourceInfo si = this.source;
				ll.add(data);
				while (true) {
					FutureInstruction<JSON> p = context.getdef(Integer.toString(cc++));
					if (p == null)
						break;
					ll.add(p.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON d = jit.next();
						System.err.println(si.toString());
						/*
						 * StringBuilder sb = new StringBuilder();
						 * sb.append(si.name).append(" "
						 * ).append(si.line).append(":").append(si.position)
						 * .append(si.endline).append(":").append(si.endposition
						 * ); System.err.println(sb.toString());
						 */
						while (jit.hasNext()) {
							JSON pi = jit.next();
							System.err.println("  " + pi.toString());
						}
						return immediateCheckedFuture(d);
					}
				});
			}

		};
	}

	public static FutureInstruction<JSON> call(SourceInfo meta) {
		meta.name = "call";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				FutureInstruction<JSON> n = context.getdef("1");
				if (n == null)
					return immediateFailedCheckedFuture(
							new ExecutionException("call() requires at least 1 parameter", meta));
				return transform(n.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						final String name = stringValue(input);
						final FutureInstruction<JSON> f = context.getdef(name);
						if (f == null)
							return immediateFailedCheckedFuture(
									new ExecutionException("call(): no function found named " + name, meta));
						List<FutureInstruction<JSON>> ll = new ArrayList<>();
						int cc = 2;
						String k;
						while (true) {
							FutureInstruction<JSON> i = context.getdef(Integer.toString(cc++));
							if (i == null)
								break;
							ll.add(i);
						}
						FutureInstruction<JSON> func = function(meta, name, ll);
						return func.call(context.getParent(), data);
					}
				});
			}

		};
	}

	// rank all
	public static FutureInstruction<JSON> defaultError(SourceInfo meta) {
		meta.name = "defaultError";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final FutureInstruction<JSON> first = context.getdef("1");
				final FutureInstruction<JSON> second = context.getdef("2");
				if (first == null) {
					JSONObject obj = context.builder().object(null);
					obj.put("status", context.builder().value(500L));
					obj.put("message", context.builder().value("unknown error"));
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
						JSONObject obj = context.builder().object(null);
						Iterator<JSON> jit = input.iterator();
						JSON f = jit.next();
						JSON s = jit.hasNext() ? jit.next() : null;
						if (f.isNumber()) {
							obj.put("status", context.builder().value(((JSONValue) f).longValue()));
							if (s != null) {
								obj.put("message", context.builder().value(((JSONValue) s).stringValue()));
							} else {
								obj.put("message", context.builder().value("an unknown error has occurred"));

							}
						} else {
							obj.put("status", context.builder().value(500L));
							obj.put("message", context.builder().value(((JSONValue) f).stringValue()));

						}
						return immediateCheckedFuture(obj);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> singleton(SourceInfo meta, final FutureInstruction<JSON> inst) {
		meta.name = "singleton";
		return new AbstractFutureInstruction(meta) {
			ListenableFuture<JSON> result = null;

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				if (result == null) {
					synchronized (this) {
						if (result == null) {
							result = inst.call(context, data);
						}
					}
				}
				return result;
			}
		};
	}

	public static FutureInstruction<JSON> fixContextData(final FutureInstruction<JSON> inst) {
		return new FixedContext(inst);
	}

	// rank all
	public static FutureInstruction<JSON> object(SourceInfo meta,
			final List<Pair<ObjectKey, FutureInstruction<JSON>>> ll, boolean forceContext) throws ExecutionException {
		boolean isContext = forceContext;

		meta.name = "object";
		if (isContext == false)
			for (Pair<ObjectKey, FutureInstruction<JSON>> ii : ll) {
				if ((!ii.f.quoted) && "_".equals(ii.f.label)) {
					isContext = true;
					break;
				}
			}
		FutureInstruction<JSON> res = isContext ? new ContextObjectInstructionFuture(meta, ll)
				: new ObjectInstructionFuture(meta, ll);
		return res;
	}

	public static FutureInstruction<JSON> match(SourceInfo meta, final FutureInstruction<JSON> l,
			FutureInstruction<JSON> r) {
		meta.name = "match";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				return transform(allAsList(l.call(context, data), r.call(context, data)),
						new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						return immediateCheckedFuture(context.builder().value(input.get(0).equals(input.get(1))));
					}
				});
			}
		};
	}

	// rank: all
	public static FutureInstruction<JSON> stepParent(SourceInfo meta) {
		meta.name = "parent";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSON p = input.getParent();
						JSON res = p == null ? JSONBuilderImpl.NULL : p;
						return immediateCheckedFuture(res);
					}
				});
			}
		};
	}

	// rank: all
	public static FutureInstruction<JSON> filter(SourceInfo meta) {
		meta.name = "filter";
		return new AbstractFutureInstruction(meta) {

			KeyedAsyncFunction<JSON, JSON, JSON> function(JSON j, JSONBuilder builder) {
				return new KeyedAsyncFunction<JSON, JSON, JSON>(j) {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input == null || input.isTrue() == false) {
							return immediateCheckedFuture(JSONBuilderImpl.NULL);
						}
						return immediateCheckedFuture(k);
					}
				};
			}

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(final JSON input) throws Exception {
						FutureInstruction<JSON> fexp = context.getdef("1");
						if (fexp != null) {
							fexp = fexp.unwrap();
							List<ListenableFuture<JSON>> ll = new ArrayList<>();
							final boolean isFrame = input instanceof JList;
							if (input instanceof JSONArray) {
								for (JSON j : (JSONArray) input) {
									ListenableFuture<JSON> jj = immediateCheckedFuture(j);
									ll.add(transform(fexp.call(context, jj), function(j, context.builder())));
								}
							} else {
								ListenableFuture<JSON> jj = immediateCheckedFuture(input);
								ll.add(transform(fexp.call(context, jj), function(input, context.builder())));
							}
							if (ll.size() == 0)
								return immediateCheckedFuture(JSONBuilderImpl.NULL);
							return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(final List<JSON> input2) throws Exception {
									JSONArray array = isFrame ? context.builder().list()
											: context.builder().array(input.getParent());

									for (JSON j : input2) {
										if (j != null && j.isTrue()) {
											array.add(j, true);
										}
									}

									return immediateCheckedFuture(array);
								}
							});
						} else {
							return immediateCheckedFuture(input);
						}
					}
				});
			}

		};
	}

	// rank: all
	public static FutureInstruction<JSON> collate(SourceInfo meta) {
		meta.name = "collate";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSONType type = input.getType();
						if (type == JSONType.ARRAY || type == JSONType.LIST) {
							Map<String, Collection<JSON>> cols = new LinkedHashMap<>();
							int c = 0;
							for (JSON j : (JSONArray) input) {
								if (j.getType() == JSONType.OBJECT) {
									for (Pair<String, JSON> pp : (JSONObject) j) {
										Collection<JSON> col = cols.get(pp.f);
										if (col == null) {
											col = new ArrayList<>();
											cols.put(pp.f, col);
										}
										int n = c - col.size();
										if (n > 0) {
											for (int i = 0; i < n; ++i) {
												col.add(JSONBuilderImpl.NULL);
											}
										}
										col.add(pp.s);

									}
								}
							}
							JSONObject obj = context.builder().object(null);
							for (Map.Entry<String, Collection<JSON>> ee : cols.entrySet()) {
								obj.put(ee.getKey(), context.builder().array(null, ee.getValue()));
							}
							return immediateCheckedFuture(obj);
						}
						return immediateCheckedFuture(input);
					}
				});
			}

		};
	}

	// rank: all
	public static FutureInstruction<JSON> sort(SourceInfo meta, boolean reverse) {
		meta.name = "sort";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					Comparator<Pair<JSON, JSON>> comparator = new Comparator<Pair<JSON, JSON>>() {

						@Override
						public int compare(Pair<JSON, JSON> o1, Pair<JSON, JSON> o2) {
							return reverse ? o2.f.compareTo(o1.f) : o1.f.compareTo(o2.f);
						}
					};

					protected ListenableFuture<JSON> sort(ArrayList<Pair<JSON, JSON>> ll, JSON original) {
						ll.sort(comparator);
						JSONArray result = original.getType() == JSONType.LIST
								? context.builder().list(original.getParent())
								: context.builder().array(original.getParent());
						for (Pair<JSON, JSON> pp : ll) {
							result.add(pp.s);
						}
						return immediateCheckedFuture(result);
					}

					@Override
					public ListenableFuture<JSON> apply(final JSON input) throws Exception {
						if (input.getType() == JSONType.ARRAY || input.getType() == JSONType.LIST) {
							FutureInstruction<JSON> fi = context.getdef("1");
							if (fi != null) {
								fi = fi.unwrap();
								List<ListenableFuture<Pair<JSON, JSON>>> ll = new ArrayList<>();
								for (JSON j : (JSONArray) input) {
									ll.add(transform(fi.call(context, immediateCheckedFuture(j)),
											new KeyedAsyncFunction<JSON, Pair<JSON, JSON>, JSON>(j) {
										public ListenableFuture<Pair<JSON, JSON>> apply(JSON ji) {
											return immediateCheckedFuture(new Pair<>(ji, k));
										}
									}));
								}
								return transform(allAsList(ll), new AsyncFunction<List<Pair<JSON, JSON>>, JSON>() {

									@Override
									public ListenableFuture<JSON> apply(List<Pair<JSON, JSON>> inp) throws Exception {
										@SuppressWarnings("unchecked")
										ArrayList<Pair<JSON, JSON>> ll = (input instanceof ArrayList)
												? (ArrayList<Pair<JSON, JSON>>) input : new ArrayList<>(inp);
										return sort(ll, input);
									}
								});
							}
							Collection<JSON> cc = ((JSONArray) input).collection();
							ArrayList<Pair<JSON, JSON>> ll = new ArrayList<>();
							for (JSON j : cc) {
								ll.add(new Pair<JSON, JSON>(j, j));
							}
							return sort(ll, input);
						} else {
							return immediateCheckedFuture(input);
						}
					}

				});
			}

		};
	}

	// rank: all
	public static FutureInstruction<JSON> stepSelf(SourceInfo meta) {
		meta.name = "self";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return immediateCheckedFuture(input);
					}
				});
			}
		};
	}

	// rank: item
	public static FutureInstruction<JSON> recursDown(SourceInfo meta) {
		meta.name = "rdown";

		return new AbstractFutureInstruction(meta) {

			protected void recurse(JSONArray unbound, JSON j) {
				// JSONType type = j.getType();
				switch (j.getType()) {
				case LIST:
				case ARRAY: {
					JSONArray a = (JSONArray) j;
					for (JSON jj : a) {
						if (jj != null) {
							unbound.add(jj);
							recurse(unbound, jj);
						}
					}
				}
					break;
				case OBJECT: {
					JSONObject a = (JSONObject) j;
					for (Pair<String, JSON> jj : a) {
						if (jj.s != null) {
							unbound.add(jj.s);
							recurse(unbound, jj.s);
						}
					}
				}
					break;
				default: // nothing
				}
			}

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JList unbound = context.builder().list();
						if (input != null) {
							recurse(unbound, input);
						}
						return immediateCheckedFuture(unbound);
					}
				});
			}
			/*
			 * // @Override public ListenableFuture<JSON>
			 * callItem(AsyncExecutionContext<JSON> context,
			 * ListenableFuture<JSON> data) throws ExecutionException { return
			 * transform(data, new AsyncFunction<JSON, JSON>() {
			 * 
			 * protected void recurse(Frame unbound, JSON j) { JSONType type =
			 * j.getType(); switch(type) { case ARRAY: { JSONArray a =
			 * (JSONArray) j; for(JSON jj : a) { unbound.add(jj);
			 * recurse(unbound, jj); } } break; case OBJECT: JSONObject a =
			 * (JSONObject) j; for(Pair<String, JSON> jj : a) {
			 * unbound.add(jj.s); recurse(unbound, jj.s); } } }
			 * 
			 * @Override public ListenableFuture<JSON> apply(JSON input) throws
			 * Exception { if(input == null) return
			 * immediateCheckedFuture(null); Frame unbound =
			 * context.builder().frame(); unbound.add(input); recurse(unbound,
			 * input); return immediateCheckedFuture(unbound); } }); }
			 */
		};
	}

	// rank all: I don't want to confuse the relationship by considering
	// children
	public static FutureInstruction<JSON> recursUp(SourceInfo meta) {
		meta.name = "rup";

		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					protected void recurse(JList unbound, JSON j) {
						if (j == null)
							return;
						unbound.add(j);
						JSON p = j.getParent();
						recurse(unbound, p);
					}

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JList unbound = context.builder().list();
						recurse(unbound, input.getParent());
						return immediateCheckedFuture(unbound);
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> type(SourceInfo meta) {
		meta.name = "children";

		return new AbstractFutureInstruction(meta) {

			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				ListenableFuture<JSON> d = data;
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null) {
					d = arg.call(context, data);
				}
				return transform(d, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return immediateCheckedFuture(context.builder().value(input.getType().toString()));
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> stepChildren(SourceInfo meta) {
		meta.name = "children";

		return new AbstractFutureInstruction(meta) {

			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JList frame = context.builder().list(null);
						this.apply(input,frame);
						return immediateCheckedFuture(frame);
					}
					public void apply(JSON input,JList frame) throws Exception {	
						switch (input.getType()) {
						case LIST:
							for (JSON j : (JSONArray) input) {
								this.apply(j, frame);
							}
							break;
						case ARRAY: {
							for (JSON j : (JSONArray) input) {
								frame.add(j);
							}
							break;
						}
						case OBJECT: {
							for (Pair<String, JSON> j : (JSONObject) input) {
								frame.add(j.s);
							}
							break;
						}
						default:
						}
					}
				});
			}

		};
	}

	public static FutureInstruction<JSON> get(SourceInfo meta, final String label) {
		meta.name = "get";

		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					ListenableFuture<JSON> get(JSONObject j) throws ExecutionException {
						JSON r = j.get(label);
						if (!(r == null || r.getType() == JSONType.NULL)) {
							return immediateCheckedFuture(r);
						}
						return null;
					}

					@Override
					public ListenableFuture<JSON> apply(final JSON input) throws Exception {
						switch (input.getType()) {
						case OBJECT: {
							JSONObject obj = (JSONObject) input;
							ListenableFuture<JSON> lf = get(obj);
							if (lf != null)
								return lf;
							return immediateCheckedFuture(JSONBuilderImpl.NULL);
						}
						case ARRAY:
						case LIST: {
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
									public ListenableFuture<JSON> apply(List<JSON> inp) throws Exception {
										JSONArray unbound = context.builder().array(input);
										for (JSON j : inp) {
											unbound.add(j);
										}
										return immediateCheckedFuture(unbound);
									}
								});
							return immediateCheckedFuture(JSONBuilderImpl.NULL);
						}
						default:
							return immediateCheckedFuture(JSONBuilderImpl.NULL);
						}
					}
				});
			}
		};
	}

	// rank: all
	public static FutureInstruction<JSON> ternary(SourceInfo meta, final FutureInstruction<JSON> c,
			final FutureInstruction<JSON> a, final FutureInstruction<JSON> b) {
		meta.name = "ternary";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
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

	/*
	 * String stringValue(JSON j) { switch(j.getType()) { case LONG: case
	 * DOUBLE: case STRING: return ((JSONValue) j).stringValue(); default: }
	 * return null; }
	 */
	// rank: all
	public static FutureInstruction<JSON> relpath(SourceInfo meta, final FutureInstruction<JSON> a,
			final FutureInstruction<JSON> b) {
		meta.name = "rpath";

		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return b.call(context, a.call(context, data));
			}

		};
	}

	// rank all
	public static FutureInstruction<JSON> abspath(SourceInfo meta, FutureInstruction<JSON> inst) {
		meta.name = "abspath";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {

				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return inst.call(context, context.getRuntime().dataContext());
					}
				});
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> sprintf(SourceInfo meta) {
		meta.name = "sprintf";
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				int cc = 1;
				while (true) {
					FutureInstruction<JSON> inst = context.getdef(Integer.toString(cc++));
					if (inst == null)
						break;
					ll.add(inst.call(context, data));
				}

				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						if (input.size() == 0)
							return data;
						ArrayList<Object> args = new ArrayList<>();
						ArrayList<Object> subargs = new ArrayList<>();
						Iterator<JSON> it = input.iterator();
						args.add(stringValue(it.next()));
						while (it.hasNext()) {
							JSON j = it.next();
							if (!j.isValue())
								subargs.add(null);
							else
								subargs.add(((JSONValue) j).get());
						}
						StringBuilder sb = new StringBuilder();
						Formatter formatter = new Formatter(sb);
						args.add(subargs);
						stringFormatter.invoke(formatter, args);
						formatter.close();
						return immediateCheckedFuture(context.builder().value(sb.toString()));
					}
				});
			}
		};
	}

	// rank all
	public static FutureInstruction<JSON> union(SourceInfo meta, final List<FutureInstruction<JSON>> seq) {
		meta.name = "union";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final List<ListenableFuture<JSON>> fut = new ArrayList<>();
				for (FutureInstruction<JSON> ii : seq) {
					fut.add(ii.call(context, data));
				}
				return transform(Futures.allAsList(fut), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSONArray unbound = context.builder().array(null, true);
						for (JSON j : input) {
							unbound.add(j);
						}
						return immediateCheckedFuture(unbound);
					}

				});
			}
		};
	}

	// rank: all
	public static FutureInstruction<JSON> dereference(SourceInfo meta, final FutureInstruction<JSON> a,
			final List<FutureInstruction<JSON>> b) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				return transform(a.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(final JSON input) throws Exception {
						AsyncFunction<List<JSON>, JSON> numf;
						int sourceSize;
						switch (input.getType()) {
						case OBJECT:
							final JSONObject theObject = (JSONObject) input;
							sourceSize = theObject.size();
							numf = new KeyedAsyncFunction<List<JSON>, JSON, JSONObject>(theObject) {
								@Override
								public ListenableFuture<JSON> apply(List<JSON> input2) throws Exception {
									final JSONArray arr = context.builder().list(null);
									Function<JSON, JSON> func = new Function<JSON, JSON>() {
										@Override
										public JSON apply(JSON t) {
											if (t instanceof JSONArray) {
												for (JSON tt : (JSONArray) t) {
													this.apply(tt);
												}
											} else {
												String key = t.stringValue();
												arr.add(k.get(key));
											}
											if (arr.size() == 1)
												return arr.get(0);
											return arr;
										}
									};
									for (JSON j : input2) {
										for (JSON jj : (JSONArray) j) {
											func.apply(jj);
										}
									}
									if (arr.size() == 1)
										return immediateCheckedFuture(arr.get(0));
									return immediateCheckedFuture(arr);
								}
							};
							break;
						case ARRAY:
						case LIST:
							final JSONArray theArray = (JSONArray) input;
							sourceSize = theArray.size();
							numf = new KeyedAsyncFunction<List<JSON>, JSON, JSONArray>((JSONArray) input) {
								@Override
								public ListenableFuture<JSON> apply(List<JSON> input2) throws Exception {
									int sourceSize = ((JSONContainer) input).size();
									final JSONArray arr = context.builder().list(null);
									Function<JSON, JSON> func = new Function<JSON, JSON>() {
										@Override
										public JSON apply(JSON jj) {
											if (jj.isNumber()) {
												int n = ((JSONValue) jj).longValue().intValue();
												if (n < 0)
													n = (n + sourceSize) % sourceSize;
												if (n < 0 || n > sourceSize) {
													arr.add(JSONBuilderImpl.NULL);
													// throw new
													// ExecutionException("index
													// out of range: " + n,
													// source);
												} else {
													arr.add(k.get(n));
												}
											} else if (jj instanceof JSONArray) {
												for (JSON jjj : (JSONArray) jj) {
													this.apply(jjj);
												}
											} else {
												arr.add(JSONBuilderImpl.NULL);
											}
											if (arr.size() == 1)
												return arr.get(0);
											return arr;
										}
									};
									for (JSON j : input2) { // per items the
															// list
										for (JSON jj : (JSONArray) j) { // per
																		// items
																		// produced
											func.apply(jj);
										}
									}
									if (arr.size() == 1)
										return immediateCheckedFuture(arr.get(0));
									return immediateCheckedFuture(arr);

								}
							};
							break;
						case STRING:
							final String str = ((JSONValue) input).stringValue();
							sourceSize = str.length();
							numf = new KeyedAsyncFunction<List<JSON>, JSON, String>(((JSONValue) input).stringValue()) {
								@Override
								public ListenableFuture<JSON> apply(List<JSON> input2) throws Exception {
									final StringBuilder sb = new StringBuilder();
									Function<JSON, JSON> func = new Function<JSON, JSON>() {

										@Override
										public JSON apply(JSON jj) {
											if (jj.isNumber()) {
												int n = ((JSONValue) jj).longValue().intValue();
												if (n < 0 || n > sourceSize) {
													// append empty string
												} else {
													sb.append(k.charAt(n));
												}
											} else if (jj instanceof JSONArray) {
												for (JSON jjj : (JSONArray) jj) {
													this.apply(jjj);
												}
											} else {
												// append the empty string
											}
											return JSONBuilderImpl.NULL;
										}
									};
									for (JSON j : input2) {
										for (JSON jj : (JSONArray) j) {
											func.apply(jj);
										}
									}
									return immediateCheckedFuture(context.builder().value(sb.toString()));

								}
							};
							break;
						default:
							throw new ExecutionException("illegal reference expression", source);
						}

						List<ListenableFuture<JSON>> ll = new ArrayList<>();
						for (FutureInstruction<JSON> ii : b) {
							if (ii instanceof RangeInstruction) {
								RangeInstruction ri = (RangeInstruction) ii;
								ri.setSize(sourceSize);
								ll.add(ri.call(context, data));
							} else {
								ll.add(transform(ii.call(context, data), new AsyncFunction<JSON, JSON>() {
									@Override
									public ListenableFuture<JSON> apply(JSON input) throws Exception {
										JSONArray arr = context.builder().array(null);
										arr.add(input);
										return immediateCheckedFuture(arr);
									}
								}));
							}
						}
						return transform(allAsList(ll), numf);
					}
				});
			};
		};
	}

	/*
	 * // rank: all public static FutureInstruction<JSON> dereference(SourceInfo
	 * meta, final FutureInstruction<JSON> a, final FutureInstruction<JSON> b) {
	 * meta.name = "deref";
	 * 
	 * return new AbstractFutureInstruction(meta) {
	 * 
	 * @SuppressWarnings("unchecked")
	 * 
	 * @Override public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON>
	 * context, ListenableFuture<JSON> data) throws ExecutionException { return
	 * transform(a.call(context, data), new AsyncFunction<JSON, JSON>() {
	 * 
	 * @Override public ListenableFuture<JSON> apply(JSON input) throws
	 * Exception { switch(input.getType()) { case LIST: case ARRAY: { JSONArray
	 * larr = (JSONArray) ra; if (rb instanceof JSONArray) { for (JSON j :
	 * (JSONArray) rb) { JSONType jtype = j.getType(); switch (jtype) { case
	 * STRING: case LONG: case DOUBLE: { Long l = ((JSONValue) j).longValue();
	 * if (l != null) { if (l < 0) l = ((l + larr.size()) % larr.size()); JSON g
	 * = larr.get(l.intValue()); if (g != null) unbound.add(g != null ? g :
	 * JSONBuilderImpl.NULL); } } break; case ARRAY: { JSONArray jarr =
	 * (JSONArray) j; if (jarr.isRanged()) { JSON ja = jarr.get(0); JSON jb =
	 * jarr.get(1); if (ja != null && ja.isValue() && jb != null &&
	 * jb.isValue()) { Long la = ((JSONValue) ja).longValue(); Long lb =
	 * ((JSONValue) jb).longValue(); if (la != null && lb != null) { // adjust
	 * for // negative index if (la < 0) la = ((la + larr.size()) %
	 * larr.size()); if (lb < 0) lb = ((lb + larr.size()) % larr.size()); int
	 * inc = la < lb ? 1 : -1; for (; (la - inc) != lb; la += inc) { JSON je =
	 * larr.get(la.intValue()); unbound.add(je != null ? je :
	 * JSONBuilderImpl.NULL); } } } } else { // a regular array for (JSON jj :
	 * jarr) { if (jj.isNumber()) { JSON je = larr.get(((JSONValue)
	 * jj).longValue().intValue()); unbound.add(je != null ? je :
	 * JSONBuilderImpl.NULL); } else { unbound.add(JSONBuilderImpl.NULL); } } }
	 * } break; default: } } } } case OBJECT: case STRING: default:
	 * 
	 * } } }); return transform(allAsList(a.call(context, data), b.call(context,
	 * data)), new AsyncFunction<List<JSON>, JSON>() {
	 * 
	 * @Override public ListenableFuture<JSON> apply(List<JSON> input) throws
	 * Exception { Iterator<JSON> it = input.iterator(); JSON ra = it.next();
	 * JSON rb = it.next(); JSONType btype = rb.getType(); if (btype ==
	 * JSONType.NULL || btype == JSONType.OBJECT) return
	 * immediateCheckedFuture(JSONBuilderImpl.NULL);
	 * 
	 * JSONArray unbound = context.builder().array(ra); switch (ra.getType()) {
	 * case LIST: case ARRAY: { break; case OBJECT: { JSONObject obj =
	 * (JSONObject) ra; if (rb instanceof JSONArray) { for (JSON j : (JSONArray)
	 * rb) { String s = j.stringValue(); JSON jj = obj.get(s); if (jj != null)
	 * unbound.add(jj); else unbound.add(JSONBuilderImpl.NULL); } } else if (rb
	 * instanceof JSONValue) { String s = rb.stringValue(); JSON jj =
	 * obj.get(s); if (jj != null) unbound.add(jj); else
	 * unbound.add(JSONBuilderImpl.NULL); } } break; case STRING: { String larr
	 * = stringValue(ra); StringBuilder sb = new StringBuilder(); if (rb
	 * instanceof JSONArray) { for (JSON j : (JSONArray) rb) { JSONType jtype =
	 * j.getType(); switch (jtype) { case STRING: case LONG: case DOUBLE: { Long
	 * l = ((JSONValue) j).longValue(); if (l != null) { if (l < 0) l = ((l +
	 * larr.length()) % larr.length()); sb.append(larr.charAt(l.intValue())); }
	 * } break; case ARRAY: { JSONArray jarr = (JSONArray) j; if
	 * (jarr.isRanged()) { JSON ja = jarr.get(0); JSON jb = jarr.get(1); if (ja
	 * != null && ja.isValue() && jb != null && jb.isValue()) { Long la =
	 * ((JSONValue) ja).longValue(); Long lb = ((JSONValue) jb).longValue(); if
	 * (la != null && lb != null) { // adjust for // negative index if (la < 0)
	 * la = ((la + larr.length()) % larr.length()); if (lb < 0) lb = ((lb +
	 * larr.length()) % larr.length()); int inc = la < lb ? 1 : -1; for (; (la -
	 * inc) != lb; la += inc) { sb.append(larr.charAt(la.intValue())); } } } }
	 * else { for (JSON jj : jarr) { if (jj.isNumber()) { Long la = ((JSONValue)
	 * jj).longValue(); la = ((la + larr.length()) % larr.length());
	 * sb.append(larr.charAt(la.intValue())); } } } } break; default: } } }
	 * return immediateCheckedFuture(context.builder().value(sb.toString())); }
	 * default: } int n = unbound.size(); if (n == 0) return
	 * immediateCheckedFuture(JSONBuilderImpl.NULL); if (n == 1) return
	 * immediateCheckedFuture(unbound.get(0)); return
	 * immediateCheckedFuture(unbound); } }); } }; }
	 */
	public static FutureInstruction<JSON> apply(SourceInfo meta) {
		meta.name = "apply";

		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final FutureInstruction<JSON> ki = context.getdef("1");
				final FutureInstruction<JSON> vi = context.getdef("2");
				final FutureInstruction<JSON> ai;
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				if (ki != null && vi != null) {
					ai = vi.unwrap();
					ll.add(ki.call(context, data));
				} else {
					ai = null;
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(final List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						final JSON inp = jit.next();
						final JSON kj = jit.hasNext() ? jit.next() : null;
						final String ks = stringValue(kj);
						if (kj != null && ai != null && inp.getType() == JSONType.OBJECT) {
							final JSONObject src = (JSONObject) inp;
							JSON param = src.get(ks);
							if (param == null)
								param = JSONBuilderImpl.NULL;
							return transform(ai.call(context, immediateCheckedFuture(inp)),
									new KeyedAsyncFunction<JSON, JSON, String>(ks) {

								@Override
								public ListenableFuture<JSON> apply(JSON in2) throws Exception {
									if (in2.getType() == JSONType.OBJECT) {
										final JSONObject obj = context.builder().object(inp.getParent());
										for (Pair<String, JSON> pp : (JSONObject) inp) {
											obj.put(pp.f, pp.s);
										}
										for (Pair<String, JSON> pp : (JSONObject) in2) {
											obj.put(pp.f, pp.s);
										}
										return immediateCheckedFuture(obj);
									}
									return immediateCheckedFuture(inp);
								}
							});
						}
						return immediateCheckedFuture(inp);
					}
				});
			};
		};
	}

	public static FutureInstruction<JSON> amend(SourceInfo meta) {
		meta.name = "amend";
		return new AbstractFutureInstruction(meta, true) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final FutureInstruction<JSON> ki = context.getdef("1");
				final FutureInstruction<JSON> vi = context.getdef("2");
				final FutureInstruction<JSON> ai;
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				if (ki != null && vi != null) {
					ai = vi.unwrap();
					ll.add(ki.call(context, data));
				} else if (ki != null) {
					ai = ki.unwrap();
				} else {
					ai = null;
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(final List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						final JSON jd = jit.next();
						final JSON kj = jit.hasNext() ? jit.next() : null;
						if (kj != null) {
							final String ks = stringValue(kj);
							if (ai != null && jd.getType() == JSONType.OBJECT) {
								context.define(":", value(immediateCheckedFuture(context.builder().value(ks)),source));
								return transform(ai.call(context, data),
										new KeyedAsyncFunction<JSON, JSON, String>(ks) {
									@Override
									public ListenableFuture<JSON> apply(JSON input) throws Exception {
										final JSONObject obj = context.builder().object(jd.getParent());
										for (Pair<String, JSON> pp : (JSONObject) jd) {
											if (!pp.f.equals(ks))
												obj.put(pp.f, pp.s);
										}
										if (input.getType() != JSONType.NULL)
											obj.put(k, input);
										return immediateCheckedFuture(obj);
									}
								});
							}
						} else {
							return transform(ai.call(context, data), new AsyncFunction<JSON, JSON>() {
								@Override
								public ListenableFuture<JSON> apply(JSON input) throws Exception {
									if (input.getType() == JSONType.OBJECT) {
										final JSONObject obj = context.builder().object(jd.getParent());
										JSONObject amend = (JSONObject) input;
										for (Pair<String, JSON> pp : (JSONObject) jd) {
											if (!amend.containsKey(pp.f))
												obj.put(pp.f, pp.s);
										}
										for (Pair<String, JSON> pp : amend) {
											if (pp.s.getType() != JSONType.NULL)
												obj.put(pp.f, pp.s);
										}
										return immediateCheckedFuture(obj);
									}
									return data;
								}
							});
						}
						return data;
					}
				});
			};
		};
	}

	/*
	 * public static InstructionFuture<JSON> fmap(SourceInfo meta) { meta.name =
	 * "omap"; return new AbstractInstructionFuture(meta,true) {
	 * 
	 * @Override public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON>
	 * context, ListenableFuture<JSON> data) throws ExecutionException {
	 * InstructionFuture<JSON> tmf = context.getdef("1"); if(tmf == null) return
	 * data; InstructionFuture<JSON> mf = tmf.unwrap(); return transform(data,
	 * new AsyncFunction<JSON, JSON>() {
	 * 
	 * @Override public ListenableFuture<JSON> apply(JSON input) throws
	 * Exception { List<ListenableFuture<Pair<JSON,JSON>>> ll =new
	 * ArrayList<>(); if(input.getType() == JSONType.OBJECT) {
	 * for(Pair<String,JSON> pp: (JSONObject) input) {
	 * AsyncExecutionContext<JSON> cc = context.createChild(false, false,
	 * immediateCheckedFuture(pp.s), meta); ll.add(mf.call(context,
	 * immediateCheckedFuture(pp.s))); } } return immediateCheckedFuture(input);
	 * } }); } }; }
	 */
	public static FutureInstruction<JSON> omap(SourceInfo meta) {
		meta.name = "omap";
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				FutureInstruction<JSON> tmf = context.getdef("1");
				if (tmf == null)
					return data;
				FutureInstruction<JSON> mf = tmf.unwrap();
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@SuppressWarnings("unchecked")
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input instanceof JSONArray) {
							List<ListenableFuture<Pair<JSON, JSON>>> ll = new ArrayList<>();
							for (JSON j : (JSONArray) input) {
								AsyncExecutionContext<JSON> cc = context.createChild(false, false,
										immediateCheckedFuture(j), meta);
								FutureInstruction<JSON> iif = value(j, meta);
								cc.define("key", iif);
								cc.define(":", iif);
								ListenableFuture<JSON> jif = immediateCheckedFuture(j);
								ll.add(transform(allAsList(jif, mf.call(cc, data)),
										new AsyncFunction<List<JSON>, Pair<JSON, JSON>>() {

									@Override
									public ListenableFuture<Pair<JSON, JSON>> apply(List<JSON> input) throws Exception {
										Iterator<JSON> jit = input.iterator();
										Pair<JSON, JSON> p = new Pair<JSON, JSON>(jit.next(), jit.next());
										return immediateCheckedFuture(p);
									}

								}));
							}
							return transform(allAsList(ll), new AsyncFunction<List<Pair<JSON, JSON>>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<Pair<JSON, JSON>> input) throws Exception {
									JSONObject obj = context.builder().object(null);
									for (Pair<JSON, JSON> pp : input) {
										if (pp.f.isValue())
											obj.put(stringValue(pp.f), pp.s);
									}
									return immediateCheckedFuture(obj);
								}
							});
						}

						return immediateCheckedFuture(JSONBuilderImpl.NULL);
					};

				});
			}
		};
	}

	public static FutureInstruction<JSON> contains(SourceInfo meta) {
		meta.name = "contains";
		return new AbstractFutureInstruction(meta) {

			@SuppressWarnings("unchecked")
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				return transform(allAsList(data, arg.call(context, data)), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON a = jit.next();
						JSON b = jit.next();
						if (a instanceof JSONArray) {
							JSONArray larr = (JSONArray) a;
							return immediateCheckedFuture(context.builder().value(larr.collection().contains(b)));
						}
						return immediateCheckedFuture(context.builder().value(false));
					}
				});
			}
		};
	}

	public static FutureInstruction<JSON> addInstruction(SourceInfo meta, FutureInstruction<JSON> a,
			FutureInstruction<JSON> b) {
		meta.name = "add";

		return dyadic(meta, a, b, new ArithmaticPolymophicOperator(meta) {
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
				for (JSON j : l.collection()) {
					arr.add(j);
				}
				for (JSON j : r.collection()) {
					arr.add(j);
				}
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

	public static FutureInstruction<JSON> subInstruction(SourceInfo meta, FutureInstruction<JSON> a,
			FutureInstruction<JSON> b) {
		meta.name = "sub";

		return dyadic(meta, a, b, new ArithmaticPolymophicOperator(meta) {
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
				for (JSON j : l) {
					if (!r.contains(j)) {
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

	public static FutureInstruction<JSON> mulInstruction(SourceInfo meta, FutureInstruction<JSON> a,
			FutureInstruction<JSON> b) {
		meta.name = "mul";
		return dyadic(meta, a, b, new ArithmaticPolymophicOperator(meta) {
			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return l * r;
			}

			@Override
			public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
				return l * r;
			}

			@Override
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONArray r) {
				JSONArray arr = builder.array(null);
				for (JSON j : l) {
					if (r.contains(j)) {
						arr.add(j);
					}
				}
				return arr;
			}
		});

	}

	public static FutureInstruction<JSON> divInstruction(SourceInfo meta, FutureInstruction<JSON> a,
			FutureInstruction<JSON> b) {
		meta.name = "div";

		return dyadic(meta, a, b, new ArithmaticPolymophicOperator(meta) {
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

	public static FutureInstruction<JSON> modInstruction(SourceInfo meta, FutureInstruction<JSON> a,
			FutureInstruction<JSON> b) {
		meta.name = "mod";
		return dyadic(meta, a, b, new ArithmaticPolymophicOperator(meta) {
			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return l % r;
			}

			@Override
			public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
				return l % r;
			}
		},true);
	}

	public static FutureInstruction<JSON> isValue(SourceInfo meta) {
		meta.name = "value";

		return isType(meta, null, JSONType.NULL, JSONType.DOUBLE, JSONType.LONG, JSONType.STRING);
	}

	public static FutureInstruction<JSON> isString(SourceInfo meta) {
		meta.name = "string";
		return isType(meta, new ConverterFunction() {

			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {
				if (input.getType() == JSONType.NULL)
					return immediateCheckedFuture(builder.value(""));
				if (input instanceof JList) {
					JList ff = builder.list();
					for (JSON jj : (JList) input) {
						if (jj instanceof JSONValue)
							ff.add(builder.value(((JSONValue) jj).stringValue()));
						else
							ff.add(builder.value(jj.toString()));
					}
					return immediateCheckedFuture(builder.value(ff.setParent(input.getParent())));
				} else if (input instanceof JSONValue) {
					return immediateCheckedFuture(
							builder.value(((JSONValue) input).stringValue()).setParent(input.getParent()));
				} else {
					return immediateCheckedFuture(builder.value(input.toString()).setParent(input.getParent()));
				}
			}
		}, JSONType.STRING);
	}

	interface NumberCheck {
		Number check(Number n);
	}

	public static FutureInstruction<JSON> isNumber(SourceInfo meta) {
		return isNumberImpl(meta, new NumberCheck() {

			@Override
			public Number check(Number n) {
				return n;
			}
		}, JSONType.LONG, JSONType.DOUBLE);
	}

	public static FutureInstruction<JSON> isReal(SourceInfo meta) {
		return isNumberImpl(meta, new NumberCheck() {

			@Override
			public Number check(Number n) {
				return n != null ? n.doubleValue() : null;
			}
		}, JSONType.DOUBLE);
	}

	public static FutureInstruction<JSON> isInt(SourceInfo meta) {
		return isNumberImpl(meta, new NumberCheck() {

			@Override
			public Number check(Number n) {
				return n != null ? n.longValue() : null;
			}
		}, JSONType.LONG);
	}

	public static FutureInstruction<JSON> isNumberImpl(SourceInfo meta, NumberCheck check, JSONType... types) {
		meta.name = "number";

		return isType(meta, new ConverterFunction() {

			JSON toNumber(JSON j) {
				Number number = null;
				switch (j.getType()) {
				case LIST: {
					JList ff = builder.list();
					JList inf = (JList) j;
					for (JSON jj : inf) {
						ff.add(toNumber(jj));
					}
					return ff;
				}
				case ARRAY:
				case OBJECT:
				case NULL:
					break;
				case LONG:
					number = ((JSONValue) j).longValue();
					break;
				case BOOLEAN:
					number = ((JSONValue) j).booleanValue() ? 1L : 0L;
					break;
				case DOUBLE:
					number = ((JSONValue) j).doubleValue();
					break;
				case STRING:
					String s = ((JSONValue) j).stringValue();
					try {
						number = Long.parseLong(s);
					} catch (NumberFormatException e) {
						try {
							number = Double.parseDouble(s);
						} catch (NumberFormatException ee) {
							number = null;
						}

					}

				}
				return number != null ? builder.value(check.check(number)) : JSONBuilderImpl.NULL;
			}

			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {
				return immediateCheckedFuture(toNumber(input).setParent(input.getParent()));
			}
		}, types);
	}

	public static FutureInstruction<JSON> isBoolean(SourceInfo meta) {
		meta.name = "boolean";
		return isType(meta, new ConverterFunction() {
			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {
				if (input instanceof JList) {
					JList ff = builder.list();
					JList inf = (JList) input;
					for (JSON jj : inf) {
						ff.add(jj);

					}
					return immediateCheckedFuture(ff.setParent(input.getParent()));
				}
				return immediateCheckedFuture(builder.value(input.isTrue()).setParent(input.getParent()));
			}
		}, JSONType.BOOLEAN);
	}

	public static FutureInstruction<JSON> isNull(SourceInfo meta) {
		meta.name = "null";

		return isType(meta, null, JSONType.NULL);
	}

	public static FutureInstruction<JSON> isArray(SourceInfo meta) {
		meta.name = "tarray";
		final JSONType[] types = { JSONType.ARRAY, JSONType.LIST };
		return isType(meta, new ConverterFunction() {

			@Override
			public ListenableFuture<JSON> apply(final JSON input) throws Exception {
				JSONType type = input.getType();
				if (type == JSONType.ARRAY) {
					return immediateCheckedFuture(input);

				}
/*				
				boolean proceed = false;
				for (JSONType t : types) {
					if (t.equals(type)) {
						proceed = true;
					}
				}
				*/
				final JSONArray destarr = builder.array(input.getParent());
				if (type == JSONType.LIST) {
					for (JSON j : (JSONArray) input) {
						destarr.add(j);
					}
					return immediateCheckedFuture(destarr);
				} else {
					destarr.add(input);
					return immediateCheckedFuture(destarr);
				}
			}

		}, types);
	}

	public static FutureInstruction<JSON> isObject(SourceInfo meta) {
		meta.name = "tobject";
		return isType(meta, null, JSONType.OBJECT);
	}

	protected static FutureInstruction<JSON> isType(SourceInfo meta, ConverterFunction conv, JSONType... types) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null && conv != null) {
					conv.setBuilder(context.builder());
					if (arg != null) {
						arg = arg.unwrap();
						FutureInstruction<JSON> a = each(meta);
						AsyncExecutionContext<JSON> fc = context.createChild(true, false, data, meta);
						fc.define("1", deferred(meta, a, context, null));
						return transform(a.call(fc, data), conv);
						// return transform(arg.call(context, data), conv);
					}
				}
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSONBuilder builder = context.builder();
						if (input instanceof JList) {
							if (hasType(JSONType.LIST)) {
								return immediateCheckedFuture(builder.value(true));
							} else {
								JSONArray arr = builder.array(null);
								for (JSON j : (JSONArray) input) {
									arr.add(builder.value(hasType(j.getType())));
								}
								return immediateCheckedFuture(arr);
							}
						}

						return immediateCheckedFuture(builder.value(hasType(input.getType())));
					}
				});
			}

			boolean hasType(JSONType type) {
				for (JSONType t : types) {
					if (type.equals(t)) {
						return true;
					}
				}
				return false;
			}

		};
	}

	public static FutureInstruction<JSON> index(SourceInfo meta, String sel) {
		return pref(meta, "#");
	}

	public static FutureInstruction<JSON> key(SourceInfo meta, String sel) {
		return pref(meta, ":");
	}

	public static FutureInstruction<JSON> pref(SourceInfo meta, String sel) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> inst = context.getdef(sel);
				if (inst == null)
					return immediateCheckedFuture(JSONBuilderImpl.NULL);
				return inst.call(context, data);
			}
		};
	}

	public static FutureInstruction<JSON> keys(SourceInfo meta) {
		return new AbstractFutureInstruction(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				AsyncFunction<JSON, JSON> f = new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input.getType() == JSONType.OBJECT) {
							Map<String, JSON> mm = ((JSONObject) input).map();
							Set<JSON> set = new LinkedHashSet<>();
							for (String s : mm.keySet()) {
								set.add(context.builder().value(s));
								// res.add(context.builder().value(s));
							}
							JSONArray res = context.builder().array(input.getParent(), set);
							return immediateCheckedFuture(res);
						}
						if (input instanceof JSONArray) {
							Set<JSON> set = new LinkedHashSet<>();
							for (JSON j : (JSONArray) input) {
								if (j.getType() == JSONType.OBJECT) {
									Map<String, JSON> mm = ((JSONObject) j).map();
									for (String s : mm.keySet()) {
										set.add(context.builder().value(s));
									}
								}
							}
							JSONArray res = context.builder().array(input.getParent(), set);
							return immediateCheckedFuture(res);
						}
						return immediateCheckedFuture(context.builder().array(null));
					}
				};
				FutureInstruction<JSON> arg = context.getdef("1");
				if (arg != null)
					return transform(arg.call(context, data), f);
				else
					return transform(data, f);
			}
		};
	}

}
