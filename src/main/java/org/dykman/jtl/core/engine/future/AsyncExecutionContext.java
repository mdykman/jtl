package org.dykman.jtl.core.engine.future;

import java.util.List;

import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.parser.JSONBuilder;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncExecutionContext<T> {
	public void define(String n,InstructionFuture<T> i);
	public ListenableFuture<T> call(String name,List<ListenableFuture<T>> args,
			ListenableFuture<T> input,AsyncExecutionContext<T> ctx)
			throws JSONException;
	public void set(String name,ListenableFuture<T> t);
	public void setDeferred(String name, InstructionFuture<T> ii,ListenableFuture<T> d);

	public ListenableFuture<T> lookup(String name)
		throws JSONException;
	public AsyncEngine<T> engine();
	
	public AsyncExecutionContext<T> createChild();
}
