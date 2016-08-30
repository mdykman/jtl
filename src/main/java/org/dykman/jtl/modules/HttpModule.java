package org.dykman.jtl.modules;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.transform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.management.RuntimeErrorException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.operator.AbstractFutureInstruction;
import org.dykman.jtl.operator.FutureInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class HttpModule extends AbstractModule {

	static Logger logger = LoggerFactory.getLogger(HttpModule.class);

	public HttpModule(String key, JSONObject config) {
		super(key, config);
	}

	@Override
	public JSON define(SourceInfo meta, AsyncExecutionContext<JSON> context, boolean serverMode) {
		context.define("get", _getInstruction(meta, context.builder()));
		context.define("post", _postInstruction(meta, context.builder()));
		context.define("put", _putInstruction(meta, context.builder()));
		context.define("delete", _deleteInstruction(meta, context.builder()));
		context.define("patch", _patchInstruction(meta, context.builder()));
		context.define("form", _formInstruction(meta, context.builder()));

		context.define("enc", new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				final ListenableFuture<JSON> in;
				if (arg != null)
					in = arg.call(context, data);
				else
					in = data;
				return transform(in, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						if (input != null) {
							// left to it's own intelligence, the standard
							// encoder
							// transforms spaces into '+' which is a dying
							// convention
							// here, we explicitly
							String s = input.stringValue();
							if (s != null) {
								String[] split = s.split(" ");
								for (int i = 0; i < split.length; ++i) {
									split[i] = URLEncoder.encode(split[i], "UTF-8");
								}
								s = String.join("%20", split);
								return Futures.immediateCheckedFuture(context.builder().value(s));
							}
						}
						return Futures.immediateCheckedFuture(context.builder().value());
					}
				});
			}
		});

		context.define("dec", new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				FutureInstruction<JSON> arg = context.getdef("1");
				final ListenableFuture<JSON> in;
				if (arg != null)
					in = arg.call(context, data);
				else
					in = data;
				return transform(in, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						String s = input.stringValue();
						return Futures.immediateCheckedFuture(context.builder().value(URLDecoder.decode(s, "UTF-8")));
					}
				});
			}
		});

		return context.builder().value(1);

	}

	abstract class MethodFactory {
		public abstract HttpMethod method(String url, JSONObject p);

		protected String stringValue(JSON j) {
			if (j == null)
				return null;
			switch (j.getType()) {
			case STRING:
			case DOUBLE:
			case LONG:
				return ((JSONValue) j).stringValue();
			default:
				return null;
			}
		}

	}

	public FutureInstruction<JSON> _formInstruction(SourceInfo meta, final JSONBuilder builder) {
		meta.name = "http:form";
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				PostMethod post = new PostMethod(url);
				if (p != null) {
					try {
						for (Pair<String, JSON> pp : p) {
							if (pp.s instanceof JSONArray) {
								for (JSON av : (JSONArray) pp.s) {
									String ss = URLEncoder.encode(av.stringValue(), "UTF-8");
									post.addParameter(pp.f, ss);
								}
							} else {
								String ss = URLEncoder.encode(pp.s.stringValue(), "UTF-8");
								post.addParameter(pp.f, ss);
							}
						}
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}

				return post;
			}
		};
		return httpInstruction(meta, mf);
	}

	public FutureInstruction<JSON> _getInstruction(SourceInfo meta, final JSONBuilder builder) {
		meta.name = "http:get";
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				HttpMethod get = new GetMethod(url);
				if (p != null) {
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					try {
						for (Pair<String, JSON> pp : p) {
							if (pp.s instanceof JSONArray) {
								for (JSON av : (JSONArray) pp.s) {
									// String ss =
									// URLEncoder.encode(av.stringValue(),
									// "UTF-8");
									String ss = av.stringValue();
									nvps.add(new NameValuePair(pp.f, ss));
								}
							} else {
								// String ss =
								// URLEncoder.encode(pp.s.stringValue(),
								// "UTF-8");
								String ss = pp.s.stringValue();
								nvps.add(new NameValuePair(pp.f, ss));
							}
						}
						// } catch (UnsupportedEncodingException e) {
						// throw new RuntimeException(e);
					} finally {
					}
					get.setQueryString(nvps.toArray(new NameValuePair[nvps.size()]));
				}
				return get;
			}
		};
		return httpInstruction(meta, mf);
	}

	public FutureInstruction<JSON> _deleteInstruction(SourceInfo meta, final JSONBuilder builder) {
		meta.name = "http:delete";
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				HttpMethod get = new DeleteMethod(url);
				if (p != null) {
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					try {
						for (Pair<String, JSON> pp : p) {
							if (pp.s instanceof JSONArray) {
								for (JSON av : (JSONArray) pp.s) {
									String ss = URLEncoder.encode(av.stringValue(), "UTF-8");
									nvps.add(new NameValuePair(pp.f, ss));
								}
							} else {
								// String ss =
								// URLEncoder.encode(pp.s.stringValue(),
								// "UTF-8");
								String ss = pp.s.stringValue();
								nvps.add(new NameValuePair(pp.f, ss));
							}
						}
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
					get.setQueryString(nvps.toArray(new NameValuePair[nvps.size()]));
				}
				return get;
			}
		};
		return httpInstruction(meta, mf);
	}

	public FutureInstruction<JSON> _postInstruction(SourceInfo meta, final JSONBuilder builder) {
		meta.name = "http:post";
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				PostMethod post = new PostMethod(url);
				if (p != null) {
					try {
						post.setRequestEntity(new StringRequestEntity(p.toString(true), "application/json", "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}
				return post;
			}
		};
		return httpInstruction(meta, mf);
	}

	public FutureInstruction<JSON> _putInstruction(SourceInfo meta, final JSONBuilder builder) {
		meta.name = "http:put";

		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				PutMethod put = new PutMethod(url);
				if (p != null) {
					try {
						put.setRequestEntity(new StringRequestEntity(p.toString(true), "application/json", "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}
				return put;
			}
		};
		return httpInstruction(meta, mf);
	}

	static class PatchMethod extends PostMethod {
		public PatchMethod(String url) {
			super(url);
		}

		@Override
		public String getName() {
			return "PATCH";
		}
	}

	public FutureInstruction<JSON> _patchInstruction(SourceInfo meta, final JSONBuilder builder) {
		meta.name = "http:patch";

		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				PatchMethod patch = new PatchMethod(url);
				if (p != null) {
					try {
						patch.setRequestEntity(new StringRequestEntity(p.toString(true), "application/json", "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}
				return patch;
			}
		};
		return httpInstruction(meta, mf);
	}

	protected FutureInstruction<JSON> httpInstruction(SourceInfo meta, final MethodFactory mf) {
		return new AbstractFutureInstruction(meta) {

			JSON read(HttpMethod m, JSONBuilder builder) throws IOException {
				Reader reader = new InputStreamReader(m.getResponseBodyAsStream());
				return builder.parse(reader);
			}

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				FutureInstruction<JSON> urli = context.getdef("1");
				ll.add(urli.call(context, data));
				FutureInstruction<JSON> args = context.getdef("2");
				if (args != null) {
					ll.add(args.call(context, data));
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

					protected int saveFile(String filename, InputStream toSave) throws Exception {
						int n;
						logger.info("saving input stream to " + filename);
						File f = new File(filename);
						File parent = f.getParentFile();
						if (parent == null) {
							parent = new File(".");
						}
						if (!parent.canWrite()) {
							logger.error("unable to write to " + parent.getAbsolutePath() + " for file " + f.getName());
							return -2;
							// throw new ExecutionException("unable to write to
							// " + parent.getAbsolutePath(), source);
						}
						try (FileOutputStream os = new FileOutputStream(f)) {
							n = IOUtils.copy(toSave, os);
						} catch (IOException e) {
							logger.error("while writing file " + f.getAbsolutePath() + ": " + e.getLocalizedMessage(),
									e);
							return -1;
						}
						return n;
					}

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON a = jit.next();
						JSON b = jit.hasNext() ? jit.next() : null;

						final String url;
						final JSONObject data;
						String file = null;
						String accept = null;
						final List<String> metaOptions = new ArrayList<>();
						if (a.getType() == JSONType.OBJECT) {
							JSONObject obj = (JSONObject) a;
							JSON p = obj.get("url");
							if (p == null) {
								logger.error("url missing in http operation");
								throw new ExecutionException("url missing in http operation", source);
							}
							url = stringValue(p);
							data = (JSONObject) obj.get("data");
							p = obj.get("file");
							if (p != null) {
								file = p.stringValue();
							}
							p = obj.get("accept");
							if (p != null) {
								accept = p.stringValue();
							}
							p = obj.get("meta");
							if (p != null) {
								if (p instanceof JSONArray) {
									JSONArray ja = (JSONArray) p;
									if (ja.size() >= 0) {
										for (JSON j : ja) {
											metaOptions.add(j.stringValue());
										}
									}
								} else {
									String s = p.stringValue();
									metaOptions.add(s);
								}

							}
						} else {
							url = stringValue(a);
							data = (JSONObject) b;
						}
						final String faccept = accept;
						final String ffile = file;
						return context.executor().submit(new Callable<JSON>() {
							@Override
							public JSON call() throws Exception {
								JSONBuilder builder = context.builder();
								HttpClient client = new HttpClient();
								HttpMethod mm = mf.method(url, data);
								logger.info(mm.getName() + " " + url);
								if (data != null)
									logger.info(data.stringValue());
								try {
									if (faccept != null) {
										mm.addRequestHeader("Accept", faccept);
									} else if (ffile != null) {
										mm.addRequestHeader("Accept", "*/*");
									} else {
										mm.addRequestHeader("Accept", "application/json,text/json");
									}
									mm.addRequestHeader("Accept-Charset", "utf-8");
									int n = client.executeMethod(mm);
									Header ct = mm.getResponseHeader("Content-Type");
									boolean json = false;
									if (ct != null) {
										String rtype = ct.getValue();
										json = rtype.contains("/json");
									}
									if (n == 200 && ffile != null) {
										return builder.value(saveFile(ffile, mm.getResponseBodyAsStream()));
									} else {
										if (json) {
											JSON jj = read(mm, context.builder());
											if (!(n >= 200 && n < 300)) {
												if (jj instanceof JSONObject) {
													((JSONObject) jj).put("__status", builder.value(n));
												}
											}
											if (metaOptions.size() > 0) {
												JSONObject metaObject = builder.object(null);
												for (String s : metaOptions) {
													switch (s) {
													case "status":
														metaObject.put("status", builder.value(n), true);
													case "headers": {
														JSONObject jh = builder.object(metaObject);
														for (Header header : mm.getResponseHeaders()) {
															jh.put(header.getName(), builder.value(header.getValue()));
														}
														metaObject.put("headers", jh, true);
													}
													}
												}
												metaObject.put("content", jj);
												jj = metaObject;
											}
											return jj;
										}
										if (!(n >= 200 && n < 300)) {
											JSONObject oo = builder.object(null);
											oo.put("status", builder.value(n));
											oo.put("content", builder.value(mm.getResponseBodyAsString()));
											return oo;
										}
										return read(mm, builder);
									}
								} catch (Exception e) {
									JSONObject oo = builder.object(null);
									oo.put("status", builder.value(503));
									oo.put("content", builder.value(e.getLocalizedMessage()));
									return oo;
								}
							}
						});
					}
				});
			}
		};

	}

}
