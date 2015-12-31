package org.dykman.jtl.operator;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public interface FutureInstruction<T> {


   public SourceInfo getSourceInfo();
	public FutureInstruction<T> unwrap();
	   public FutureInstruction<JSON> getBareInstruction();
	   
	public ListenableFuture<T> call(AsyncExecutionContext<T> context, ListenableFuture<T> data)
			throws ExecutionException;
}
