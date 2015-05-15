package org.dykman.jtl.core.engine;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncEngine<T> extends MapFactory<String,T>, CollectionFactory<T> {
	public ListenableFuture<T> execute(T c,InstructionFuture<T> i);


	// defined a named function
	public void define(String n,InstructionFuture<T> i);
	
	// call a named function
	public ListenableFuture<T> call(String name,List<ListenableFuture<T>> args,ListenableFuture<T> context);

	public void set(String name,ListenableFuture<T> t);
	public ListenableFuture<T> lookup(String name);

	public boolean is_boolean(T t);
	public Number number(T t);
	public String string(T t);
	public String is_string(T t);
	public T append(T a,T b);

	public boolean bool(T t);
	public boolean equals(T l,T r);
	public int compare(T l,T r);
	
	public ExecutorService getExecutorService();
}
