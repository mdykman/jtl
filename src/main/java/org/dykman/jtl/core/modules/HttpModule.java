package org.dykman.jtl.core.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpParams;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.Module;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.Pair;
import org.dykman.jtl.core.engine.ExecutionException;
import org.dykman.jtl.core.engine.future.AbstractInstructionFuture;
import org.dykman.jtl.core.engine.future.AsyncExecutionContext;
import org.dykman.jtl.core.engine.future.InstructionFuture;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import static com.google.common.util.concurrent.Futures.*;
public class HttpModule implements Module {

	public HttpModule() {
		// TODO Auto-generated constructor stub
	}
	@Override
	public void define(AsyncExecutionContext<JSON> parent, JSONBuilder builder,JSONObject config) {
		ListeningExecutorService les = parent.executor();
		
		parent.define("get", getInstruction(les,builder));
		parent.define("post", postInstruction(les,builder));
		parent.define("form", formInstruction(les,builder));
		
	}
	
	InstructionFuture<JSON> formInstruction(final ListeningExecutorService les,final JSONBuilder builder) {
		return new AbstractInstructionFuture() {
			
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				InstructionFuture<JSON> urli = context.getdef("1");
				ll.add(urli.call(context, data));
				InstructionFuture<JSON> args = context.getdef("2");
				if(args!=null) {
					ll.add(args.call(context, data));
				}
				return transform(allAsList(ll),new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON a = jit.next();
						JSON b = jit.hasNext() ? jit.next(): null;
						
						final String url;
						final JSONObject data;
						if(a.getType() == JSONType.OBJECT) {
							JSONObject obj = (JSONObject) a;
							url = stringValue(obj.get("url"));
							data = (JSONObject) obj.get("data");
						} else {
							url = stringValue(a);
							data  = (JSONObject) b;
						}
						Callable<JSON> runit = new Callable<JSON>() {
							
							@Override
							public JSON call() throws Exception {
							
								HttpClient client = new HttpClient();
								PostMethod post = new PostMethod(url);
								if(data!=null) {
									List <NameValuePair> nvps = new ArrayList <NameValuePair>();
									for(Pair<String,JSON> pp: data) {
										nvps.add(new NameValuePair(pp.f, pp.s.toString()));
									}
								}
								post.addRequestHeader("Accept", "application/json");
								int n = client.executeMethod(post);
								if(n>=200 && n < 300) {
									JSONObject oo = builder.object(null);
									oo.put("status", builder.value(n));
									oo.put("content",builder.value(post.getResponseBodyAsString()));
									return oo;
								}
								return builder.parse(post.getResponseBodyAsStream());
							}
						};
						return les.submit(runit);
					}
				});
			}
		};
		
	
	}	
	InstructionFuture<JSON> getInstruction(final ListeningExecutorService les,final JSONBuilder builder) {
		return new AbstractInstructionFuture() {
			
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				InstructionFuture<JSON> urli = context.getdef("1");
				ll.add(urli.call(context, data));
				InstructionFuture<JSON> args = context.getdef("2");
				if(args!=null) {
					ll.add(args.call(context, data));
				}
				return transform(allAsList(ll),new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON a = jit.next();
						JSON b = jit.hasNext() ? jit.next(): null;
						
						final String url;
						final JSONObject data;
						if(a.getType() == JSONType.OBJECT) {
							JSONObject obj = (JSONObject) a;
							url = stringValue(obj.get("url"));
							data = (JSONObject) obj.get("data");
						} else {
							url = stringValue(a);
							data  = (JSONObject) b;
						}
						Callable<JSON> runit = new Callable<JSON>() {
							
							@Override
							public JSON call() throws Exception {
								HttpClient client = new HttpClient();
								HttpMethod get = new GetMethod(url);
								if(data!=null) {
									HttpParams params = get.getParams();
									for(Pair<String,JSON> pp: (JSONObject)data) {
										params.setParameter(pp.f, stringValue(pp.s));
									}
								}
								get.addRequestHeader("Accept", "application/json");
								int n = client.executeMethod(get);
								if(!(n>=200 && n < 300)) {
									JSONObject oo = builder.object(null);
									oo.put("status", builder.value(n));
									oo.put("content",builder.value(get.getResponseBodyAsString()));
									return oo;
								}
								return builder.parse(get.getResponseBodyAsStream());
							}
						};
						return les.submit(runit);
					}
				});
			}
		};
		
	}
	
	InstructionFuture<JSON> postInstruction(final ListeningExecutorService les,final JSONBuilder builder) {
		return new AbstractInstructionFuture() {
			
			@Override
			public ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
				throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				InstructionFuture<JSON> urli = context.getdef("1");
				ll.add(urli.call(context, data));
				InstructionFuture<JSON> args = context.getdef("2");
				if(args!=null) {
					ll.add(args.call(context, data));
				}
				return transform(allAsList(ll),new AsyncFunction<List<JSON>, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						Iterator<JSON> jit = input.iterator();
						JSON a = jit.next();
						JSON b = jit.hasNext() ? jit.next(): null;
						
						final String url;
						final JSONObject data;
						if(a.getType() == JSONType.OBJECT) {
							JSONObject obj = (JSONObject) a;
							url = stringValue(obj.get("url"));
							data = (JSONObject) obj.get("data");
						} else {
							url = stringValue(a);
							data  = (JSONObject) b;
						}
						Callable<JSON> runit = new Callable<JSON>() {
							
							@Override
							public JSON call() throws Exception {
							
								HttpClient client = new HttpClient();
								
								PostMethod post = new PostMethod(url);
								if(data!=null) {
									post.setRequestEntity(
										new StringRequestEntity(data.toString(),"application/json","UTF-8"));
								}
								post.addRequestHeader("Accept", "application/json");
								int n = client.executeMethod(post);
								if(n>=200 && n < 300) {
									JSONObject oo = builder.object(null);
									oo.put("status", builder.value(n));
									oo.put("content",builder.value(post.getResponseBodyAsString()));
									return oo;
								}
								return builder.parse(post.getResponseBodyAsStream());
							}
						};
						return les.submit(runit);
					}
				});
			}
		};
		
	}

}
