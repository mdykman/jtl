package org.dykman.jtl.core.engine.future;

import java.util.List;

import org.dykman.jtl.core.JSONBuilderImpl;
import org.dykman.jtl.core.JSONException;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncExecutionContext<T> {
	public void define(String n,InstructionFuture<T> i);
	public ListenableFuture<T> call(String name, ListenableFuture<T> input)
			throws JSONException;
	
	public void setDeferred(String name, DeferredCall def);
	public DeferredCall getDeferred(String name);

	public void set(String name,ListenableFuture<T> t);
	public ListenableFuture<T> lookup(String name)
		throws JSONException;
	public AsyncEngine<T> engine();
	
	public AsyncExecutionContext<T> createChild();
}
