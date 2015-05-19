package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.engine.ExecutionException;
import org.dykman.jtl.core.parser.JSONBuilder;

import com.google.common.util.concurrent.ListenableFuture;

public class SimpleExecutionContext implements AsyncExecutionContext<JSON> {
	
	final AsyncExecutionContext<JSON> parent;
	final AsyncEngine<JSON> engine;
	//final JSON context;
	Map<String,InstructionFuture<JSON>> functions = new HashMap<>();
	
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
	public ListenableFuture<JSON> call(final String name,
			final List<ListenableFuture<JSON>> args, final ListenableFuture<JSON> input, final AsyncExecutionContext<JSON> ctx) 
			throws JSONException {
		InstructionFuture<JSON> i = functions.get(name);
		if(i!=null) {
			i.call(ctx, input);
		} 
		if(parent != null) return parent.call(name, args, input,ctx);
		// TODO: function not found!!
		return immediateFailedCheckedFuture(new ExecutionException("function " + name + " not found"));
//		return immediateFuture(new JSONValue(null));
	}

	@Override
	public void set(String name, ListenableFuture<JSON> t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ListenableFuture<JSON> lookup(String name) {
		if(parent!=null) parent.lookup(name);
		return immediateFuture(new JSONValue(null));
	}
	@Override
	public AsyncExecutionContext<JSON> createChild() {
		// TODO Auto-generated method stub
		return null;
	}


}
