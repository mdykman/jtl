package org.dykman.jtl.core.engine.future;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncExecutionContext<T> {
	public void define(String n,InstructionFuture<T> i);

	public InstructionFuture<T> getdef(String name);
//	public ListenableFuture<T> call(String name, ListenableFuture<T> input)
//			throws JSONException;
	
//	public void setDeferred(String name, InstructionFuture<T> def);
//	public DeferredCall getDeferred(String name);

	public void set(String name,ListenableFuture<T> t);
	public ListenableFuture<T> lookup(String name,ListenableFuture<T> t)
		throws ExecutionException;
	
	public AsyncExecutionContext<T> getNamedContext(String label);
	public AsyncExecutionContext<T> getNamedContext(String label,boolean create);
//	public AsyncEngine<T> engine();
	
	public AsyncExecutionContext<T> createChild(boolean fc);
}
