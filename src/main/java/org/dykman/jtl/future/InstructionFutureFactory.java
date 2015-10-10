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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JList;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;
import org.dykman.jtl.modules.ModuleLoader;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class InstructionFutureFactory {

	protected static final String JTL_INTERNAL = "_jtl_internal_";
	// protected static final String JTL_INTERNAL_KEY = JTL_INTERNAL + "key_";

	protected static final Method stringFormatter;

	static {
		try {
			stringFormatter = Formatter.class.getMethod("format", String.class, Object[].class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ExceptionInInitializerError("unable to load formatter");
		}
	}
	// rank all

	public static InstructionFuture<JSON> memo(SourceInfo meta, final InstructionFuture<JSON> inst) {
		if (inst instanceof MemoInstructionFuture)
			return inst;
		meta.name = "memo";
		return new MemoInstructionFuture(meta, inst);
	}

	/*
	 * public JSONBuilder builder() { return builder; }
	 */
	// rank: all

	public static InstructionFuture<JSON> file(SourceInfo meta) {
		meta.name = "file";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				InstructionFuture<JSON> f = context.getdef("1");
				return transform(f.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						final InstructionFuture<JSON> dd = context.getdef("2");
						final File ff = context.file(stringValue(input));
						if (dd == null) {
							Callable<JSON> cc = new Callable<JSON>() {
								@Override
								public JSON call() throws Exception {
									if (ff.exists())
										return context.builder().parse(ff);
									System.err.println("failed to find file " + ff.getPath());
									return context.builder().value();
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

	public static InstructionFuture<JSON> rand(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				InstructionFuture<JSON> f = context.getdef("1");
				InstructionFuture<JSON> s = context.getdef("2");
				ArrayList<ListenableFuture<JSON>> d = new ArrayList<>();
				d.add(data);
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
						JSON parent = jit.next();
						int n = input.size() - 1;
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
						if(js==null) {
							return immediateCheckedFuture(builder.value());
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

	public static InstructionFuture<JSON> url(SourceInfo meta) {
		meta.name = "url";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				InstructionFuture<JSON> f = context.getdef("1");
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
	public static InstructionFuture<JSON> map(SourceInfo meta) {
		meta.name = "map";

		return new AbstractInstructionFuture(meta,true) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				InstructionFuture<JSON> gbe = context.getdef("1");

				final InstructionFuture<JSON> mapfunc;
				if (gbe != null) {
					mapfunc = gbe.unwrap();
				} else {
					return immediateCheckedFuture(context.builder().value());
				}
				AsyncFunction<JSON, JSON> asy =new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(final JSON input) throws Exception {
						switch (input.getType()) {
						case OBJECT: {
							JSONObject obj = (JSONObject) input;
							List<ListenableFuture<Pair<String, JSON>>> ll = new ArrayList<>();

							for (Pair<String, JSON> pp : obj) {
								AsyncExecutionContext<JSON> ctx = context.createChild(false, false, data, meta);
								ctx.define("0", value(context.builder().value("map"), getSourceInfo()));
								InstructionFuture<JSON> ki = value(context.builder().value(pp.f), getSourceInfo());
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
							List<ListenableFuture<JSON>> ll =new ArrayList<>();
							for(JSON jj:(JSONArray)input) {
								ll.add(mapfunc.call(context, immediateCheckedFuture(jj)));
							}
							return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<JSON> linput) throws Exception {
									JSONArray array = context.builder().array(input.getParent());
									for(JSON j: linput) {
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
	public static InstructionFuture<JSON> groupBy(SourceInfo meta) {
		meta.name = "group";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
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
	public static InstructionFuture<JSON> importInstruction(SourceInfo meta, final JSON conf) {
		meta.name = "import";
		return new AbstractInstructionFuture(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>(3);
				// InstructionFuture<JSON> key =
				// context.getdef(JTL_INTERNAL_KEY);
				InstructionFuture<JSON> key = context.getdef(":");

				ll.add(key.call(context, data));
				int cc = 1;
				while (true) {
					InstructionFuture<JSON> files = context.getdef(Integer.toString(cc++));
					if (files == null)
						break;
					ll.add(files.call(context, data));
				}

				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						final String key = stringValue(jit.next());
						final List<InstructionFuture<JSON>> imperitives = new ArrayList<>(input.size());

						AsyncExecutionContext<JSON> mc = context.getInit().getNamedContext(key, true, false, source);
						// TODO:: associate a compiler to the context
						final JtlCompiler compiler = new JtlCompiler(context.builder(), false, false, false);
						// List<ListenableFuture<JSON>> ll = new ArrayList<>();
						InstructionFuture<JSON> initInst = null;
						while (jit.hasNext()) {
							String ff = stringValue(jit.next());
							File toParse = new File(context.currentDirectory(), ff);
							InstructionFuture<JSON> inst = compiler.parse(toParse);
							if(inst instanceof FixedContext) {
								FixedContext fi = (FixedContext) inst;
								inst = fi.getIntruction();
							if (inst instanceof ObjectInstructionBase) {
								ObjectInstructionBase oib = (ObjectInstructionBase) inst;
								List<Pair<String, InstructionFuture<JSON>>> prs = oib.pairs();
								for (Pair<String, InstructionFuture<JSON>> ip : prs) {
									String k = ip.f;
									InstructionFuture<JSON> fii = fixContextData(ip.s);
									if ("!init".equals(k)) {
										initInst = InstructionFutureFactory.memo(fii.getSourceInfo(), fii);
										mc.define(k.substring(1), initInst);
									} else if ("_".equals(k)) {
										context.getInit().define(key, fii);
									} else {
										char ic = k.charAt(0);
										if (ic == '$') {
											mc.define(k.substring(1), InstructionFutureFactory
													.deferred(fii.getSourceInfo(), fii, context, data));
										} else if (ic == '!') {
											InstructionFuture<JSON> imp = InstructionFutureFactory
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
							AsyncFunction<List<JSON>, JSON> impSync = new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
									JSONArray arr = context.builder().array(null);
									for (JSON j : input) {
										arr.add(j);
									}
									return immediateCheckedFuture(arr);
								}
							};

							if (initInst != null) {
								AsyncFunction<JSON, JSON> func = new AsyncFunction<JSON, JSON>() {

									@Override
									public ListenableFuture<JSON> apply(JSON input) throws Exception {
										List<ListenableFuture<JSON>> ll = new ArrayList<>();

										for (InstructionFuture<JSON> ii : imperitives) {
											ll.add(ii.call(context, data));

										}
										return transform(allAsList(ll), impSync);
									}
								};

							} else {
								List<ListenableFuture<JSON>> ll = new ArrayList<>();
								for (InstructionFuture<JSON> ii : imperitives) {
									ll.add(ii.call(context, data));
								}
								return transform(allAsList(ll), impSync);
							}
						}

						return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> list) throws Exception {
								JSONArray out = context.builder().array(null);
								for (JSON j : list) {
									out.add(j);
								}
								return immediateCheckedFuture(out);
							}
						});

					}
				});
			}

		};
	}

	
	// rank all
	public static InstructionFuture<JSON> loadModule(SourceInfo meta, final JSONObject conf) {
		meta.name = "loadmodule";
		JSONObject modconf = (JSONObject) conf.get("modules");
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(
					final AsyncExecutionContext<JSON> context, 
					final ListenableFuture<JSON> data)
					throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>(12);
				InstructionFuture<JSON> key = context.getdef(":");
				InstructionFuture<JSON> name = context.getdef("1");
				ll.add(key.call(context, data));
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
						JSONObject config = (JSONObject) (jit.hasNext() ? jit.next() : context.builder().object(null));
						
						JSON smode = conf.get("server-mode");
						JSONObject modules = (JSONObject)conf.get("modules");
		//				String klassName = modules.get(name).stringValue();
						
						ModuleLoader ml = ModuleLoader.getInstance(context.currentDirectory(),context.builder(), conf);
						
						AsyncExecutionContext<JSON> modctx = context.getInit().getNamedContext(key, true, false, meta);
						JSON n = ml.create(meta, name,key, modctx, smode.isTrue(),config);
						return immediateCheckedFuture(n);
					}
				});

			}
		};

	}

	public static InstructionFuture<JSON> reMatch(SourceInfo meta, final String p, final InstructionFuture<JSON> d) {
		meta.name = "rematch";
		final Pattern pattern = Pattern.compile(p);
		return new AbstractInstructionFuture(meta, true) {

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
	public static InstructionFuture<JSON> negate(SourceInfo meta, InstructionFuture<JSON> ii) {
		meta.name = "negate";
		// TODO:: need to actually apply negation here: boring but eventually
		// necessary
		return ii;
	}

	// rank: all
	public static InstructionFuture<JSON> deferred(SourceInfo meta, InstructionFuture<JSON> inst,
			AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> t) {
		SourceInfo si = inst.getSourceInfo().clone();
		si.name = "deferred";
		return memo(meta, new DeferredCall(si, inst, context, t));
	}

	public static InstructionFuture<JSON> thread(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> arg = context.getdef("1");
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

	public static InstructionFuture<JSON> sum(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				InstructionFuture<JSON> arg = context.getdef("1");
				if (arg != null) {
					data = arg.call(context, data);
				}
				// final ListenableFuture<JSON> d=data;
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						// System.err.println("sum is passed a " +
						// input.getClass().getName());
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
						return Futures.immediateCheckedFuture(context.builder().value());
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> avg(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				InstructionFuture<JSON> arg = context.getdef("1");
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
						return Futures.immediateCheckedFuture(context.builder().value());
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> min(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				InstructionFuture<JSON> arg = context.getdef("1");
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
						return Futures.immediateCheckedFuture(context.builder().value());
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> max(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				InstructionFuture<JSON> arg = context.getdef("1");
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
						return Futures.immediateCheckedFuture(context.builder().value());
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> append(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				int cc = 1;
				InstructionFuture<JSON> inst = null;
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

	public static InstructionFuture<JSON> defined(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> inst = context.getdef("1");
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

	public static InstructionFuture<JSON> each(SourceInfo meta) {
		return new AbstractInstructionFuture(meta, true) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final InstructionFuture<JSON> inst = context.getdef("1").unwrap();
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						return inst.call(context, immediateCheckedFuture(input));
						/*
						 * if(input instanceof JSONArray) {
						 * List<ListenableFuture<JSON>> ll = new ArrayList<>();
						 * for(JSON j : (JSONArray) input) {
						 * ll.add(inst.call(context,
						 * immediateCheckedFuture(j))); } return
						 * transform(allAsList(ll), new
						 * AsyncFunction<List<JSON>, JSON>() {
						 * 
						 * @Override public ListenableFuture<JSON>
						 * apply(List<JSON> input) throws Exception { Frame f =
						 * context.builder().frame(); for(JSON j : input) {
						 * f.add(j); } return immediateCheckedFuture(f); } }); }
						 * else { return inst.call(context,
						 * immediateCheckedFuture(input)); }
						 */
					}

				});
			}
		};
	}

	public static InstructionFuture<JSON> variable(SourceInfo meta, final String name) {
		final FunctionInstruction fi = function(meta, name, null);
		fi.setVariable(true);
		return new AbstractInstructionFuture(fi.getSourceInfo()) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				try {
					return fi.call(context, data);
				} catch (ExecutionException e) {
					// eat the access exception and quietly return null
					return immediateCheckedFuture(context.builder().value());
				}
			}
		};
	}

	// rank all
	public static InstructionFuture<JSON> activate(SourceInfo meta, final String name,
			final List<InstructionFuture<JSON>> iargs) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {

				InstructionFuture<JSON> inst = context.getdef(name);
				if (inst == null) {
					throw new ExecutionException(source);
				}
				// inst = inst.unwrap(context);
				// if(inst instanceof DeferredCall) {
				inst = deref(context, data, inst, iargs);

				// }
				return inst.call(context, data);
			}

			public InstructionFuture<JSON> deref(AsyncExecutionContext<JSON> ctx, ListenableFuture<JSON> data,
					InstructionFuture<JSON> inst, List<InstructionFuture<JSON>> ll) {
				AsyncExecutionContext<JSON> cc = ctx.createChild(true, false, data, source);
				int ctr = 1;
				for (InstructionFuture<JSON> i : ll) {
					InstructionFuture<JSON> ins = wrapArgument(source, ctx, i, data);
					cc.define(Integer.toString(ctr++), ins);
				}

				DeferredCall dc = new DeferredCall(source, inst.getBareInstruction(), cc, null);
				return dc;

			}

		};
	}

	public static InstructionFuture<JSON> paramArray(SourceInfo meta, final List<InstructionFuture<JSON>> iargs) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				for (InstructionFuture<JSON> ii : iargs) {
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

	static InstructionFuture<JSON> wrapArgument(SourceInfo info, AsyncExecutionContext<JSON> ctx,
			InstructionFuture<JSON> inst, final ListenableFuture<JSON> data) {
		return memo(info, deferred(info, inst, ctx.declaringContext(), data));

	}

	public static FunctionInstruction function(SourceInfo meta, final String name,
			final List<InstructionFuture<JSON>> iargs) {
		meta.name = "function";
		return new FunctionInstruction(meta, name, iargs);
	}

	// rank all
	public static InstructionFuture<JSON> value(final ListenableFuture<JSON> o, SourceInfo meta) {
		meta.name = "value";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return o;
			}
		};
	}

	// rank all
	public static InstructionFuture<JSON> value(JSON o, SourceInfo meta) {
		return value(immediateCheckedFuture(o), meta);
	}

	// rank all
	public static InstructionFuture<JSON> value(JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(), meta);
	}

	// rank all
	public static InstructionFuture<JSON> value(final Boolean val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static InstructionFuture<JSON> value(final Long val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static InstructionFuture<JSON> value(final Double val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static InstructionFuture<JSON> value(final String val, JSONBuilder builder, SourceInfo meta) {
		return value(builder.value(val), meta);
	}

	// rank all
	public static InstructionFuture<JSON> number(Number num, JSONBuilder builder, SourceInfo meta) {
		meta.name = "number";
		return value(builder.value(num), meta);
	}

	public static InstructionFuture<JSON> listToArray(final InstructionFuture<JSON> inst, SourceInfo meta) {
		meta.name = "l2f";
		return new AbstractInstructionFuture(meta) {
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
	public static InstructionFuture<JSON> array(final List<InstructionFuture<JSON>> ch, SourceInfo meta) {
		meta.name = "array";
		return new AbstractInstructionFuture(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> t) throws ExecutionException {
				List<ListenableFuture<JSON>> args = new ArrayList<>();
				for (InstructionFuture<JSON> i : ch) {
					if (i instanceof RangeInstruction) {
						args.add(i.call(context, t));
					} else {
						args.add(i.call(context, t));
					}
				}
				return transform(allAsList(args), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSONArray arr = context.builder().array(null, input.size());
						for (JSON j : input) {
							if (j.getType() == JSONType.LIST) {
								JList jl = (JList) j;
								for (JSON jj : jl) {
									arr.add(jj == null ? context.builder().value() : jj);
								}
							} else {
								arr.add(j == null ? context.builder().value() : j);
							}
						}
						return immediateCheckedFuture(arr);
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> dyadic(SourceInfo meta, InstructionFuture<JSON> left,
			InstructionFuture<JSON> right, DyadicAsyncFunction<JSON> f) {
		// already writeen by the function provider
		// meta.name="dyadic";
		return new AbstractInstructionFuture(meta, true) {

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

	// rank all
	public static InstructionFuture<JSON> unique(SourceInfo meta) {
		meta.name = "unique";
		return new AbstractInstructionFuture(meta) {
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
							return immediateCheckedFuture(input.cloneJSON());
						}
					}

				});
			}

		};
	}

	public static InstructionFuture<JSON> substr(SourceInfo meta) {
		meta.name = "substr";

		return new AbstractInstructionFuture(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> exp1 = context.getdef("1");
				if (exp1 == null) {
					return data;
				}
				final InstructionFuture<JSON> exp2 = context.getdef("2");
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

	public static InstructionFuture<JSON> split(SourceInfo meta) {
		meta.name = "split";

		return new AbstractInstructionFuture(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> exp = context.getdef("1");
				if (exp == null) {
					return data;
				}
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				ll.add(exp.call(context, data));
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

	public static InstructionFuture<JSON> write(SourceInfo meta) {
		meta.name = "write";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final InstructionFuture<JSON> arg = context.getdef("1");
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

	public static InstructionFuture<JSON> copy(SourceInfo meta) {
		meta.name = "copy";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> arg = context.getdef("1");
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
								arr.add(d.cloneJSON());
							}
							return immediateCheckedFuture(arr);
						}
						return immediateCheckedFuture(d);
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> join(SourceInfo meta) {
		meta.name = "join";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final InstructionFuture<JSON> arg = context.getdef("1");
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
	public static InstructionFuture<JSON> count(SourceInfo meta) {
		meta.name = "count";
		return new AbstractInstructionFuture(meta) {
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
				InstructionFuture<JSON> arg = context.getdef("1");
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
	public static InstructionFuture<JSON> conditional(final InstructionFuture<JSON> test,
			final InstructionFuture<JSON> trueI, final InstructionFuture<JSON> falseI, SourceInfo meta) {
		meta.name = "conditional";
		return new AbstractInstructionFuture(meta, true) {

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
	public static InstructionFuture<JSON> chain(SourceInfo meta, final InstructionFuture<JSON>... inst) {
		meta.name = "chain";
		InstructionFuture<JSON> chain = null;
		for (InstructionFuture<JSON> ii : inst) {
			if (chain == null) {
				chain = ii;
			} else {
				final InstructionFuture<JSON> pp = chain;
				chain = new AbstractInstructionFuture(meta) {

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
	public static InstructionFuture<JSON> sequence(SourceInfo meta, final InstructionFuture<JSON>... inst) {
		meta.name = "sequence";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				for (InstructionFuture<JSON> ii : inst) {
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
	public static InstructionFuture<JSON> params(SourceInfo meta) {
		meta.name = "params";
		return new AbstractInstructionFuture(meta) {

			@SuppressWarnings("unchecked")
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
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
				return sequence(meta, (InstructionFuture<JSON>[]) ll.toArray(new InstructionFuture[ll.size()]))
						.call(context, data);
			}

		};

	}

	public static InstructionFuture<JSON> switchInst(SourceInfo meta) {
		meta.name = "switch";
		return new AbstractInstructionFuture(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				final InstructionFuture<JSON> f = context.getdef("1");
				final InstructionFuture<JSON> s = context.getdef("2");
				if (f == null || s == null)
					return immediateFailedCheckedFuture(new ExecutionException("switch missing parameters", source));
				InstructionFuture<JSON> body = s.getBareInstruction();
				if (body instanceof ObjectInstructionBase) {
					final ObjectInstructionBase base = (ObjectInstructionBase) body;
					final AsyncExecutionContext<JSON> pcontext = context.getParent();
					return transform(f.call(pcontext, data), new AsyncFunction<JSON, JSON>() {
						@Override
						public ListenableFuture<JSON> apply(JSON input) throws Exception {
							String str = stringValue(input);
							InstructionFuture<JSON> defi = null;
							for (Pair<String, InstructionFuture<JSON>> pp : base.pairs()) {
								if ("_".equals(pp.f)) {
									defi = pp.s;
								}
								if (str.equals(pp.f)) {
									return pp.s.call(pcontext, data);
								}
							}
							if (defi != null) {
								return defi.call(pcontext, data);
							}
							return immediateCheckedFuture(context.builder().value());
						}
					});
				} else
					return immediateFailedCheckedFuture(
							new ExecutionException("switch expects an object as a second parameter", meta));
			}

		};
	}

	public static InstructionFuture<JSON> trace(SourceInfo meta) {
		meta.name = "trace";
		return new AbstractInstructionFuture(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(data);
				InstructionFuture<JSON> p = context.getdef("1");
				if (p != null)
					ll.add(p.call(context, data));
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON d = jit.next();
						JSON pi = jit.hasNext() ? jit.next() : null;
						if (pi == null)
							System.err.println(input.toString());
						else
							System.err.println(pi.toString(((JSONValue) pi).longValue().intValue(), false));
						return immediateCheckedFuture(d);
					}
				});
			}

		};
	}

	public static InstructionFuture<JSON> call(SourceInfo meta) {
		meta.name = "call";
		return new AbstractInstructionFuture(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				InstructionFuture<JSON> n = context.getdef("1");
				if (n == null)
					return immediateFailedCheckedFuture(
							new ExecutionException("call() requires at least 1 parameter", meta));
				return transform(n.call(context, data), new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						final String name = stringValue(input);
						final InstructionFuture<JSON> f = context.getdef(name);
						if (f == null)
							return immediateFailedCheckedFuture(
									new ExecutionException("call(): no function found named " + name, meta));
						List<InstructionFuture<JSON>> ll = new ArrayList<>();
						int cc = 2;
						String k;
						while (true) {
							InstructionFuture<JSON> i = context.getdef(Integer.toString(cc++));
							if (i == null)
								break;
							ll.add(i);
						}
						InstructionFuture<JSON> func = function(meta, name, ll);
						return func.call(context.getParent(), data);
					}
				});
			}

		};
	}

	// rank all
	public static InstructionFuture<JSON> defaultError(SourceInfo meta) {
		meta.name = "defaultError";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> first = context.getdef("1");
				final InstructionFuture<JSON> second = context.getdef("2");
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

	public static InstructionFuture<JSON> singleton(SourceInfo meta, final InstructionFuture<JSON> inst) {
		meta.name = "singleton";
		return new AbstractInstructionFuture(meta) {
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

	public static InstructionFuture<JSON> fixContextData(final InstructionFuture<JSON> inst) {
		return new FixedContext(inst);
	}

	// rank all
	public static InstructionFuture<JSON> object(SourceInfo meta, final List<Pair<String, InstructionFuture<JSON>>> ll,
			boolean forceContext) throws ExecutionException {
		boolean isContext = forceContext;
		List<Pair<String, InstructionFuture<JSON>>> fields = new ArrayList<>();
		List<InstructionFuture<JSON>> imperitives = new ArrayList<>(ll.size());

		meta.name = "object";
		if (isContext == false)
			for (Pair<String, InstructionFuture<JSON>> ii : ll) {
				String k = ii.f;
				if (k.charAt(0) == '$') {
				} else if (k.charAt(0) == '!') {
				}
				if ("_".equals(ii.f)) {
					isContext = true;
					break;
				}
			}
		return isContext ? new ContextObjectInstructionFuture(meta, ll) : new ObjectInstructionFuture(meta, ll);
	}

	// rank: all
	public static InstructionFuture<JSON> stepParent(SourceInfo meta) {
		meta.name = "parent";
		return new AbstractInstructionFuture(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JSON p = input.getParent();
						JSON res = p == null ? context.builder().value() : p;
						return immediateCheckedFuture(res);
					}
				});
			}
		};
	}

	// rank: all
	public static InstructionFuture<JSON> filter(SourceInfo meta) {
		meta.name = "filter";
		return new AbstractInstructionFuture(meta) {

			KeyedAsyncFunction<JSON, JSON, JSON> function(JSON j, JSONBuilder builder) {
				return new KeyedAsyncFunction<JSON, JSON, JSON>(j) {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input == null || input.isTrue() == false) {
							return immediateCheckedFuture(builder.value());
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
						InstructionFuture<JSON> fexp = context.getdef("1");
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
								return immediateCheckedFuture(context.builder().value());
							return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(final List<JSON> input2) throws Exception {
									JSONArray array = isFrame ? context.builder().frame()
											: context.builder().array(input.getParent());
									if (input2.size() == 1) {
										JSON j = input2.iterator().next();
										if (j != null && j.isTrue()) {
											array.add(j, true);
										}
									} else {
										for (JSON j : input2) {
											if (j != null && j.isTrue()) {
												array.add(j, true);
											}
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
	public static InstructionFuture<JSON> collate(SourceInfo meta) {
		meta.name = "collate";
		return new AbstractInstructionFuture(meta) {

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
												col.add(context.builder().value());
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
	public static InstructionFuture<JSON> sort(SourceInfo meta, boolean reverse) {
		meta.name = "sort";
		return new AbstractInstructionFuture(meta) {

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
								? context.builder().frame(original.getParent())
								: context.builder().array(original.getParent());
						for (Pair<JSON, JSON> pp : ll) {
							result.add(pp.s);
						}
						return immediateCheckedFuture(result);
					}

					@Override
					public ListenableFuture<JSON> apply(final JSON input) throws Exception {
						if (input.getType() == JSONType.ARRAY || input.getType() == JSONType.LIST) {
							InstructionFuture<JSON> fi = context.getdef("1");
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
	public static InstructionFuture<JSON> stepSelf(SourceInfo meta) {
		meta.name = "self";
		return new AbstractInstructionFuture(meta) {

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
	public static InstructionFuture<JSON> recursDown(SourceInfo meta) {
		meta.name = "rdown";

		return new AbstractInstructionFuture(meta) {

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
						JList unbound = context.builder().frame();
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
	public static InstructionFuture<JSON> recursUp(SourceInfo meta) {
		meta.name = "rup";

		return new AbstractInstructionFuture(meta) {
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
						JList unbound = context.builder().frame();
						recurse(unbound, input.getParent());
						return immediateCheckedFuture(unbound);
					}
				});
			}
		};
	}

	/*
	 * // rank all public static InstructionFuture<JSON> mapChildren(SourceInfo
	 * meta) { return new AbstractInstructionFuture(meta) {
	 * 
	 * @Override public ListenableFuture<JSON> call(AsyncExecutionContext<JSON>
	 * context, ListenableFuture<JSON> data) throws ExecutionException { return
	 * transform(data, new AsyncFunction<JSON, JSON>() {
	 * 
	 * @Override public ListenableFuture<JSON> apply(JSON input) throws
	 * Exception { switch(input.getType()) { case FRAME: case ARRAY: { Frame
	 * frame = context.builder().frame(); for(JSON j : (JSONArray) input) {
	 * frame.add(j); } return immediateCheckedFuture(frame);
	 * 
	 * } case OBJECT: { JSONObject obj = context.builder().object(null);
	 * for(Pair<String, JSON> j : (JSONObject) input) { obj.put(j.f, j.s); }
	 * return immediateCheckedFuture(obj);
	 * 
	 * } default: } return immediateCheckedFuture(context.builder().value());
	 * 
	 * } }); } }; }
	 */
	public static InstructionFuture<JSON> stepChildren(SourceInfo meta) {
		meta.name = "children";

		return new AbstractInstructionFuture(meta) {

			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						JList frame = context.builder().frame(input);
						switch (input.getType()) {
						case LIST: {
							for (JSON j : (JSONArray) input) {
								switch (j.getType()) {
								case ARRAY:
								case LIST:
									for (JSON k : (JSONArray) j) {
										frame.add(k);
									}
									break;
								// return immediateCheckedFuture(frame);
								case OBJECT: {
									frame.add(j);
									// for (Pair<String, JSON> pp : (JSONObject)
									// j) {
									// frame.add(pp.s);
									// }
								}
								default:
								}
							}
							break;
						}
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
						return immediateCheckedFuture(frame);
					}
				});
			}

		};
	}

	public static InstructionFuture<JSON> get(SourceInfo meta, final String label) {
		meta.name = "get";

		return new AbstractInstructionFuture(meta) {

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
							return immediateCheckedFuture(context.builder().value());
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
							return immediateCheckedFuture(context.builder().value());
						}
						default:
							return immediateCheckedFuture(context.builder().value());
						}
					}
				});
			}
		};
	}

	// rank: all
	public static InstructionFuture<JSON> ternary(SourceInfo meta, final InstructionFuture<JSON> c,
			final InstructionFuture<JSON> a, final InstructionFuture<JSON> b) {
		meta.name = "ternary";
		return new AbstractInstructionFuture(meta) {

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
	public static InstructionFuture<JSON> relpath(SourceInfo meta, final InstructionFuture<JSON> a,
			final InstructionFuture<JSON> b) {
		meta.name = "rpath";

		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				return b.call(context, a.call(context, data));
			}

		};
	}

	// rank all
	public static InstructionFuture<JSON> abspath(SourceInfo meta, InstructionFuture<JSON> inst) {
		meta.name = "abspath";
		return new AbstractInstructionFuture(meta) {
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
	public static InstructionFuture<JSON> sprintf(SourceInfo meta) {
		meta.name = "sprintf";
		return new AbstractInstructionFuture(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				int cc = 1;
				while (true) {
					InstructionFuture<JSON> inst = context.getdef(Integer.toString(cc++));
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
	public static InstructionFuture<JSON> union(SourceInfo meta, final List<InstructionFuture<JSON>> seq) {
		meta.name = "union";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final List<ListenableFuture<JSON>> fut = new ArrayList<>();
				for (InstructionFuture<JSON> ii : seq) {
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
	public static InstructionFuture<JSON> dereference(SourceInfo meta, final InstructionFuture<JSON> a,
			final InstructionFuture<JSON> b) {
		meta.name = "deref";

		return new AbstractInstructionFuture(meta) {

			@SuppressWarnings("unchecked")
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
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
							return immediateCheckedFuture(context.builder().value());

						JSONArray unbound = context.builder().array(ra);
						switch (ra.getType()) {
						case LIST:
						case ARRAY: {
							JSONArray larr = (JSONArray) ra;
							if (rb instanceof JSONArray) {
								for (JSON j : (JSONArray) rb) {
									JSONType jtype = j.getType();
									switch (jtype) {
									case STRING:
									case LONG:
									case DOUBLE: {
										Long l = ((JSONValue) j).longValue();
										if (l != null) {
											if (l < 0)
												l = ((l + larr.size()) % larr.size());
											JSON g = larr.get(l.intValue());
											if (g != null)
												unbound.add(g != null ? g : context.builder().value());
										}
									}
										break;
									case ARRAY: {
										JSONArray jarr = (JSONArray) j;
										if (jarr.size() == 2) {
											JSON ja = jarr.get(0);
											JSON jb = jarr.get(1);
											if (ja != null && ja.isValue() && jb != null && jb.isValue()) {
												Long la = ((JSONValue) ja).longValue();
												Long lb = ((JSONValue) jb).longValue();
												if (la != null && lb != null) {
													// adjust for
													// negative index
													if (la < 0)
														la = ((la + larr.size()) % larr.size());
													if (lb < 0)
														lb = ((lb + larr.size()) % larr.size());
													int inc = la < lb ? 1 : -1;
													for (; (la - inc) != lb; la += inc) {
														unbound.add(larr.get(la.intValue()));
													}
												}
											}
										}
									}
										break;
									default:
									}
								}
							}
						}
							break;
						case OBJECT: {
							JSONObject obj = (JSONObject) ra;
							if (rb instanceof JSONArray) {
								for (JSON j : (JSONArray) rb) {
									JSONType jtype = j.getType();
									switch (jtype) {
									case STRING:
										// case LONG:
										// case DOUBLE:
										String s = ((JSONValue) j).stringValue();
										JSON jj = obj.get(s);
										if (jj != null)
											unbound.add(jj);
										break;
									default:
									}
								}
							}
						}
							break;
						case STRING: {
							String larr = stringValue(ra);
							StringBuilder sb = new StringBuilder();
							if (rb instanceof JSONArray) {
								for (JSON j : (JSONArray) rb) {
									JSONType jtype = j.getType();
									switch (jtype) {
									case STRING:
									case LONG:
									case DOUBLE: {
										Long l = ((JSONValue) j).longValue();
										if (l != null) {
											if (l < 0)
												l = ((l + larr.length()) % larr.length());
											sb.append(larr.charAt(l.intValue()));
										}
									}
										break;
									case ARRAY: {
										JSONArray jarr = (JSONArray) j;
										if (jarr.size() == 2) {
											JSON ja = jarr.get(0);
											JSON jb = jarr.get(1);
											if (ja != null && ja.isValue() && jb != null && jb.isValue()) {
												Long la = ((JSONValue) ja).longValue();
												Long lb = ((JSONValue) jb).longValue();
												if (la != null && lb != null) {
													// adjust for
													// negative index
													if (la < 0)
														la = ((la + larr.length()) % larr.length());
													if (lb < 0)
														lb = ((lb + larr.length()) % larr.length());
													int inc = la < lb ? 1 : -1;
													for (; (la - inc) != lb; la += inc) {
														sb.append(larr.charAt(la.intValue()));
													}
												}
											}
										}
									}
										break;
									default:
									}
								}
							}
							return immediateCheckedFuture(context.builder().value(sb.toString()));
						}
						default:
						}
						int n = unbound.size();
						if (n == 0)
							return immediateCheckedFuture(context.builder().value());
						if (n == 1)
							return immediateCheckedFuture(unbound.get(0));
						return immediateCheckedFuture(unbound);
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> apply(SourceInfo meta) {
		meta.name = "apply";

		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> ki = context.getdef("1");
				final InstructionFuture<JSON> vi = context.getdef("2");
				final InstructionFuture<JSON> ai;
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
								param = context.builder().value();
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

	public static InstructionFuture<JSON> amend(SourceInfo meta) {
		meta.name = "amend";

		return new AbstractInstructionFuture(meta, true) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,final ListenableFuture<JSON> data)
					throws ExecutionException {
				final InstructionFuture<JSON> ki = context.getdef("1");
				final InstructionFuture<JSON> vi = context.getdef("2");
				final InstructionFuture<JSON> ai;
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
								return transform(ai.call(context, data),
										new KeyedAsyncFunction<JSON, JSON, String>(ks) {

									@Override
									public ListenableFuture<JSON> apply(JSON input) throws Exception {

										final JSONObject obj = context.builder().object(jd.getParent());
										// context.builder().o
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
							return transform(ai.call(context, data),
									new AsyncFunction<JSON, JSON>() {

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
	public static InstructionFuture<JSON> fmap(SourceInfo meta) {
		meta.name = "omap";
		return new AbstractInstructionFuture(meta,true) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				InstructionFuture<JSON> tmf = context.getdef("1");
				if(tmf == null) return data;
				InstructionFuture<JSON> mf = tmf.unwrap();
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						List<ListenableFuture<Pair<JSON,JSON>>> ll =new ArrayList<>();
						if(input.getType() == JSONType.OBJECT) {
							for(Pair<String,JSON> pp: (JSONObject) input) {
								AsyncExecutionContext<JSON> cc = context.createChild(false, false, immediateCheckedFuture(pp.s), meta);
								ll.add(mf.call(context, immediateCheckedFuture(pp.s)));
							}
						}
						return immediateCheckedFuture(input);
					}
				});
			}
		};
	}
	*/
	public static InstructionFuture<JSON> omap(SourceInfo meta) {
		meta.name = "omap";
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				InstructionFuture<JSON> tmf = context.getdef("1");
				if(tmf == null) return data;
				InstructionFuture<JSON> mf = tmf.unwrap();
				return transform(data, new AsyncFunction<JSON, JSON>() {

					@SuppressWarnings("unchecked")
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input instanceof JSONArray) {
							List<ListenableFuture<Pair<JSON, JSON>>> ll = new ArrayList<>();
							for (JSON j : (JSONArray) input) {
								AsyncExecutionContext<JSON> cc = context.createChild(false, false, immediateCheckedFuture(j), meta);
								InstructionFuture<JSON> iif = value(j, meta);
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

						return immediateCheckedFuture(context.builder().value());
					};

				});
			}
		};
	}

	public static InstructionFuture<JSON> contains(SourceInfo meta) {
		meta.name = "contains";
		return new AbstractInstructionFuture(meta) {

			@SuppressWarnings("unchecked")
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				InstructionFuture<JSON> arg = context.getdef("1");
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

	public static InstructionFuture<JSON> addInstruction(SourceInfo meta, InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		meta.name = "add";

		return dyadic(meta, a, b, new DefaultPolymorphicOperator(meta) {
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
			public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
				JSONArray arr = builder.array(null);

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

	public static InstructionFuture<JSON> subInstruction(SourceInfo meta, InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		meta.name = "sub";

		return dyadic(meta, a, b, new DefaultPolymorphicOperator(meta) {
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

	public static InstructionFuture<JSON> mulInstruction(SourceInfo meta, InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		meta.name = "mul";
		return dyadic(meta, a, b, new DefaultPolymorphicOperator(meta) {
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

	public static InstructionFuture<JSON> divInstruction(SourceInfo meta, InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		meta.name = "div";

		return dyadic(meta, a, b, new DefaultPolymorphicOperator(meta) {
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

	public static InstructionFuture<JSON> modInstruction(SourceInfo meta, InstructionFuture<JSON> a,
			InstructionFuture<JSON> b) {
		meta.name = "mod";
		return dyadic(meta, a, b, new DefaultPolymorphicOperator(meta) {
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

	public static InstructionFuture<JSON> isValue(SourceInfo meta) {
		meta.name = "value";

		return isType(meta, null, JSONType.NULL, JSONType.DOUBLE, JSONType.LONG, JSONType.STRING);
	}

	static abstract class ConverterFunction implements AsyncFunction<JSON, JSON> {
		JSONBuilder builder;

		public void setBuilder(JSONBuilder b) {
			builder = b;
		}
	}

	public static InstructionFuture<JSON> isString(SourceInfo meta) {
		meta.name = "string";
		return isType(meta, new ConverterFunction() {

			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {
				if (input.getType() == JSONType.NULL)
					return immediateCheckedFuture(builder.value(""));
				if (input instanceof JList) {
					JList ff = builder.frame();
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

	public static InstructionFuture<JSON> isNumber(SourceInfo meta) {
		return isNumberImpl(meta, new NumberCheck() {

			@Override
			public Number check(Number n) {
				return n;
			}
		}, JSONType.LONG, JSONType.DOUBLE);
	}

	public static InstructionFuture<JSON> isReal(SourceInfo meta) {
		return isNumberImpl(meta, new NumberCheck() {

			@Override
			public Number check(Number n) {
				return n != null ? n.doubleValue() : null;
			}
		}, JSONType.DOUBLE);
	}

	public static InstructionFuture<JSON> isInt(SourceInfo meta) {
		return isNumberImpl(meta, new NumberCheck() {

			@Override
			public Number check(Number n) {
				return n != null ? n.longValue() : null;
			}
		}, JSONType.LONG);
	}

	public static InstructionFuture<JSON> isNumberImpl(SourceInfo meta, NumberCheck check, JSONType... types) {
		meta.name = "number";

		return isType(meta, new ConverterFunction() {

			JSON toNumber(JSON j) {
				Number number = null;
				switch (j.getType()) {
				case LIST: {
					JList ff = builder.frame();
					JList inf = (JList) j;
					for (JSON jj : inf) {
						ff.add(builder.value(toNumber(jj)));
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
				return number != null ? builder.value(check.check(number)) : builder.value();
			}

			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {
				return immediateCheckedFuture(toNumber(input).setParent(input.getParent()));
			}
		}, types);
	}

	public static InstructionFuture<JSON> isBoolean(SourceInfo meta) {
		meta.name = "boolean";
		return isType(meta, new ConverterFunction() {
			@Override
			public ListenableFuture<JSON> apply(JSON input) throws Exception {
				if (input instanceof JList) {
					JList ff = builder.frame();
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

	public static InstructionFuture<JSON> isNull(SourceInfo meta) {
		meta.name = "null";

		return isType(meta, null, JSONType.NULL);
	}

	public static InstructionFuture<JSON> isArray(SourceInfo meta) {
		meta.name = "tarray";
		final JSONType[] types = { JSONType.ARRAY, JSONType.LIST };
		return isType(meta, new ConverterFunction() {

			@Override
			public ListenableFuture<JSON> apply(final JSON input) throws Exception {
				JSONType type = input.getType();
				if (type == JSONType.ARRAY) {
					return immediateCheckedFuture(input);

				}
				boolean proceed = false;
				for (JSONType t : types) {
					if (t.equals(type)) {
						proceed = true;
					}
				}
				final JSONArray destarr = builder.frame();
				if (proceed) {
					if (type == JSONType.LIST) {
						List<ListenableFuture<JSON>> ll = new ArrayList<>();
						for (JSON j : (JSONArray) input) {
							ll.add(apply(j));
						}
						return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> inp) throws Exception {
								for (JSON j : inp) {
									destarr.add(j);
								}
								return immediateCheckedFuture(destarr);
							}
						});
					}
				} else {
					destarr.add(input);

				}
				return immediateCheckedFuture(destarr);
			}

		}, types);
	}

	public static InstructionFuture<JSON> isObject(SourceInfo meta) {
		meta.name = "tobject";
		return isType(meta, null, JSONType.OBJECT);
	}

	protected static InstructionFuture<JSON> isType(SourceInfo meta, ConverterFunction conv, JSONType... types) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				InstructionFuture<JSON> arg = context.getdef("1");
				if (arg != null && conv != null) {
					conv.setBuilder(context.builder());
					if (arg != null) {
						arg = arg.unwrap();
						InstructionFuture<JSON> a = each(meta);
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

	public static InstructionFuture<JSON> index(SourceInfo meta, String sel) {
		return pref(meta, "#");
	}

	public static InstructionFuture<JSON> key(SourceInfo meta, String sel) {
		return pref(meta, ":");
	}

	public static InstructionFuture<JSON> pref(SourceInfo meta, String sel) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				InstructionFuture<JSON> inst = context.getdef(sel);
				if (inst == null)
					return immediateCheckedFuture(context.builder().value());
				return inst.call(context, data);
			}
		};
	}

	public static InstructionFuture<JSON> keys(SourceInfo meta) {
		return new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				AsyncFunction<JSON, JSON> f = new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input.getType() == JSONType.OBJECT) {
							Map<String, JSON> mm = ((JSONObject) input).map();
							JSONArray res = context.builder().array(input.getParent());
							for (String s : mm.keySet()) {
								res.add(context.builder().value(s));
							}
							return immediateCheckedFuture(res);
						}
						return immediateCheckedFuture(context.builder().value());
					}
				};
				InstructionFuture<JSON> arg = context.getdef("1");
				if (arg != null)
					return transform(arg.call(context, data), f);
				else
					return transform(data, f);
			}
		};
	}

}
