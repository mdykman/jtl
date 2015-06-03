package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

public class SimpleExecutionContext implements AsyncExecutionContext<JSON> {
	
	final AsyncExecutionContext<JSON> parent;
	Map<String,InstructionFuture<JSON>> functions = new ConcurrentHashMap<>();
	
	public SimpleExecutionContext(AsyncEngine<JSON> engine) {
		this.parent = null;
	}
	public SimpleExecutionContext(AsyncExecutionContext<JSON> parent) {
		this.parent = parent;
	}
	@Override
	public void define(String n, InstructionFuture<JSON> i) {
		functions.put(n, i);
	}
	@Override
	public void set(String name, ListenableFuture<JSON> t) {
		functions.put(name, InstructionFutureFactory.value(t));
	}

	@Override
	public AsyncExecutionContext<JSON> createChild() {
		return new SimpleExecutionContext(this);
	}
	@Override
	public InstructionFuture<JSON> getdef(String name) {
		return functions.get(name);
	}
	@Override
	public ListenableFuture<JSON> lookup(String name,ListenableFuture<JSON> t) throws ExecutionException {
		InstructionFuture<JSON> inst = getdef(name);
		if(inst!=null) {
			return inst.call(this, t);
		}
		System.err.println("lookup failed: " + name);
		return immediateFuture(null);
	}


}
