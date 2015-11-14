package org.dykman.jtl.ext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AbstractFutureInstruction;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.FutureInstruction;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class Loader {

	AsyncExecutionContext<JSON> context;
	public Loader(AsyncExecutionContext<JSON> c) {
		context = c;
	}

	public int load(Class<?> klass,JSONObject global,JSONObject modConfig) 
			throws Exception {
		Method[] methods = klass.getDeclaredMethods();
		Constructor<?> cc = klass.getDeclaredConstructor(JSONObject.class,JSONObject.class);
		Object obj = cc.newInstance(global,modConfig);
		for(Method m:methods) {
			Annotation[] annotations = m.getDeclaredAnnotations();
			for(Annotation a: annotations) {
				if(a instanceof JtlMethod) {
					if(validateMethod(m)) {
						String name = ((JtlMethod) a).name();
						if(name == null) {
							name = m.getName();
						}
						context.define(name, createMethod(obj,m));
					}
				}
			}
		}
		return 0;
	}
	
	protected boolean validateMethod(Method method) {
		return true;
	}	
	
	protected FutureInstruction<JSON> createMethod(
			final Object o,
			final Method m) {
		SourceInfo si = SourceInfo.internal("loader");
		return new AbstractFutureInstruction(si) {
			
			@Override
			public ListenableFuture<JSON> _call(
					final AsyncExecutionContext<JSON> context, 
					final ListenableFuture<JSON> data)
					throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				int cc = 1;
				while(true) {
					FutureInstruction<JSON> fi = context.getdef(Integer.toString(cc));
					if(fi == null) break;
					ll.add(fi.call(context, data));
					++cc;
				}
				
				return Futures.transform(Futures.allAsList(ll), 
						new AsyncFunction<List<JSON>, JSON>() {
							@Override
							public ListenableFuture<JSON> apply(List<JSON> params) throws Exception {
								return Futures.immediateCheckedFuture((JSON)m.invoke(context,o, params));
							}
						});
			}
		};
	}

}
