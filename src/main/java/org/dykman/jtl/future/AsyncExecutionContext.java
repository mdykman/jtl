package org.dykman.jtl.future;

import java.io.File;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSONBuilder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public interface AsyncExecutionContext<T> {
	public void define(String n,InstructionFuture<T> i);
	
	public ListeningExecutorService executor();

	public JSONBuilder builder();
	public AsyncExecutionContext<T> getMasterContext();


	public File currentDirectory();
	public File file(String in);
	public ListenableFuture<T> config();

	public ListenableFuture<T> dataContext();

	public AsyncExecutionContext<T> getParent();

	public InstructionFuture<T> getdef(String name);

	public ListenableFuture<T> lookup(String name,ListenableFuture<T> t)
		throws ExecutionException;
	
	public AsyncExecutionContext<T> getNamedContext(String label);
	public AsyncExecutionContext<T> getNamedContext(String label,boolean create);
	
	public AsyncExecutionContext<T> createChild(boolean fc);
}
