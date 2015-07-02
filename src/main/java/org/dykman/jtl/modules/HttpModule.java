package org.dykman.jtl.modules;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.transform;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.future.AbstractInstructionFuture;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;
import org.dykman.jtl.json.JSON.JSONType;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class HttpModule implements Module {

	final JSONObject baseConfig;

	public HttpModule(JSONObject config) {
		baseConfig = config;
	}

	@Override
	public void define(AsyncExecutionContext<JSON> context) {
		context.define("get", _getInstruction(context.builder()));
		context.define("post", _postInstruction(context.builder()));
		context.define("put", _putInstruction(context.builder()));
		context.define("delete", _deleteInstruction(context.builder()));
		context.define("form", _formInstruction(context.builder()));

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

	public InstructionFuture<JSON> _formInstruction(
			final JSONBuilder builder) {
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				PostMethod post = new PostMethod(url);
				if (p != null) {
					for (Pair<String, JSON> pp : p) {
						post.addParameter(pp.f, pp.s.toString());
					}
				}

				return post;
			}
		};
		return httpInstruction(builder, mf);
	}

	public InstructionFuture<JSON> _getInstruction(
			final JSONBuilder builder) {
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				HttpMethod get = new GetMethod(url);
				if (p != null) {
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					for (Pair<String, JSON> pp : p) {
						nvps.add(new NameValuePair(pp.f, pp.s.toString()));
					}
					get.setQueryString(nvps.toArray(new NameValuePair[nvps
							.size()]));
				}
				return get;
			}
		};
		return httpInstruction(builder, mf);
	}

	public InstructionFuture<JSON> _deleteInstruction(
			final JSONBuilder builder) {
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				HttpMethod get = new DeleteMethod(url);
				if (p != null) {
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					for (Pair<String, JSON> pp : p) {
						nvps.add(new NameValuePair(pp.f, pp.s.toString()));
					}
					get.setQueryString(nvps.toArray(new NameValuePair[nvps
							.size()]));
				}
				return get;
			}
		};
		return httpInstruction(builder, mf);
	}

	public InstructionFuture<JSON> _postInstruction(
			final JSONBuilder builder) {
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				PostMethod post = new PostMethod(url);
				if (p != null) {
					try {
						// IOUtils.cop
						// post.setr
						post.setRequestEntity(new StringRequestEntity(p
								.toString(true), "application/json", "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}
				return post;
			}
		};
		return httpInstruction(builder, mf);
	}

	public InstructionFuture<JSON> _putInstruction(
			final JSONBuilder builder) {
		MethodFactory mf = new MethodFactory() {

			@Override
			public HttpMethod method(String url, JSONObject p) {
				PutMethod put = new PutMethod(url);
//				PostMethod post = new PostMethod(url);
				if (p != null) {
					try {
						// IOUtils.cop
						// post.setr
						put.setRequestEntity(new StringRequestEntity(p
								.toString(), "application/json", "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}
				return put;
			}
		};
		return httpInstruction(builder, mf);
	}

	protected InstructionFuture<JSON> httpInstruction(
			 final JSONBuilder builder,
			final MethodFactory mf) {
		return new AbstractInstructionFuture() {

			JSON read(HttpMethod m) 
				throws IOException {
				Reader reader= new InputStreamReader(m
					.getResponseBodyAsStream());
			// try anyways as no  error was signaled, but response type incorrect
			return builder.parse(reader);				
			}
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				InstructionFuture<JSON> urli = context.getdef("1");
				ll.add(urli.call(context, data));
				InstructionFuture<JSON> args = context.getdef("2");
				if (args != null) {
					ll.add(args.call(context, data));
				}
				return transform(allAsList(ll),
						new AsyncFunction<List<JSON>, JSON>() {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws Exception {
								Iterator<JSON> jit = input.iterator();
								JSON a = jit.next();
								JSON b = jit.hasNext() ? jit.next() : null;

								final String url;
								final JSONObject data;
								if (a.getType() == JSONType.OBJECT) {
									JSONObject obj = (JSONObject) a;
									url = stringValue(obj.get("url"));
									data = (JSONObject) obj.get("data");
								} else {
									url = stringValue(a);
									data = (JSONObject) b;
								}
								return context.executor().submit(new Callable<JSON>() {
									@Override
									public JSON call() throws Exception {
										HttpClient client = new HttpClient();
										HttpMethod mm = mf.method(url, data);
										try {
											mm.addRequestHeader("Accept",
													"application/json");
											mm.addRequestHeader("Accept-Charset",
												"utf-8");
											int n = client.executeMethod(mm);
											Header ct = mm
													.getResponseHeader("Content-Type");
											String rtype = ct.getValue();
											boolean json = rtype
													.contains("/json");
											if (json) {
												JSON jj = read(mm);
												if (!(n >= 200 && n < 300)) {
													if (jj instanceof JSONObject) {
														((JSONObject) jj)
																.put("__status",
																		builder.value(n));
													}
												}
												return jj;
											}
											if (!(n >= 200 && n < 300)) {
												JSONObject oo = builder
														.object(null);
												oo.put("status",
														builder.value(n));
												oo.put("content",
														builder.value(mm
																.getResponseBodyAsString()));
												return oo;
											}
											return read(mm);
										} catch (Exception e) {
											JSONObject oo = builder
													.object(null);
											oo.put("status", builder.value(503));
											oo.put("content", builder.value(e
													.getLocalizedMessage()));
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
