package org.dykman.jtl.future;

import java.io.File;
import java.util.Map;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
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

   public String method();
   public String method(String m);
	public ListenableFuture<T> dataContext();

	public AsyncExecutionContext<T> getParent();

   public AsyncExecutionContext<T> declaringContext();
   public AsyncExecutionContext<T> declaringContext(AsyncExecutionContext<T> c);

   public boolean isFunctionContext() ;
	
	public boolean debug();
   public boolean debug(boolean d);
	public int counter(String label, int increment);
	public InstructionFuture<T> getdef(String name);

	public void inject(AsyncExecutionContext<T> cc);
   public void inject(String name,AsyncExecutionContext<T> cc);
	public AsyncExecutionContext<T> getNamedContext(String label);
	public AsyncExecutionContext<T> getNamedContext(String label,boolean create,SourceInfo info);
   public Map<String, AsyncExecutionContext<JSON>>getNamedContexts();
	public AsyncExecutionContext<T> createChild(boolean fc,ListenableFuture<T> dataContext,SourceInfo source);
}
