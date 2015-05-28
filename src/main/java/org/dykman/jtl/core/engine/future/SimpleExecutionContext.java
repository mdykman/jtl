package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONBuilderImpl;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

public class SimpleExecutionContext<JSON> implements AsyncExecutionContext<JSON> {
	
	final AsyncExecutionContext<JSON> parent;
	final AsyncEngine<JSON> engine;
	//final JSON context;
	Map<String,InstructionFuture<JSON>> functions = new ConcurrentHashMap<>();
	Map<String,Duo<InstructionFuture<JSON>,ListenableFuture<JSON>>> deferred = new ConcurrentHashMap<>();
	Map<String,ListenableFuture<JSON>> variables = new ConcurrentHashMap<>();
	
	public SimpleExecutionContext(AsyncEngine<JSON> engine) {
		this.engine = engine;
		this.parent = null;
	}
	public SimpleExecutionContext(AsyncExecutionContext<JSON> parent) {
		this.engine = parent.engine();
		this.parent = parent;
	}
	/*
	public JSONBuilder builder() {
		return engine
	}
	*/
	public AsyncEngine<JSON> engine() {
		return engine;
	}

	@Override
	public void define(String n, InstructionFuture<JSON> i) {
		functions.put(n, i);
	}

	@Override
	public ListenableFuture<JSON> call(
			final String name,
			final List<ListenableFuture<JSON>> args, 
			final ListenableFuture<JSON> input, 
			final AsyncExecutionContext<JSON> ctx) 
			throws JSONException {
		InstructionFuture<JSON> i = functions.get(name);
		if(i!=null) return i.call(ctx, input);
		if(parent!=null) return parent.call(name, args, input,ctx);
		return immediateFailedCheckedFuture(new JSONException("function " + name + " not found"));
	}

	@Override
	public void set(String name, ListenableFuture<JSON> t) {
		variables.put(name, t);
		
	}

//	@SuppressWarnings("unchecked")
	@Override
	public ListenableFuture<JSON> lookup(String name)
		throws JSONException {
		ListenableFuture<JSON> res = variables.get(name);
		synchronized(deferred) {
			res = variables.get(name);
			if(res == null) {
				Duo<InstructionFuture<JSON>,ListenableFuture<JSON>> d = deferred.get(name);
				if(d != null) {
					res = d.first.call(this,d.second);
					variables.put(name, res);
				}
			}
		}
		if(res == null && parent!=null) res = parent.lookup(name);
		if(res!=null) return res;
		return immediateFailedCheckedFuture(new JSONException("variable " + name + " not found"));
	}
	@Override
	public AsyncExecutionContext<JSON> createChild() {
		return new SimpleExecutionContext<>(this);
	}
	@Override
	public void setDeferred(String name, InstructionFuture<JSON> ii,ListenableFuture<JSON> d) {
		deferred.put(name, new Duo<InstructionFuture<JSON>, ListenableFuture<JSON>>(ii, d));		
	}


}
