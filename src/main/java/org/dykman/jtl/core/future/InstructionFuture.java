package org.dykman.jtl.core.future;

import org.dykman.jtl.core.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

public interface InstructionFuture<T> {

	public InstructionFuture<T> unwrap();
	public ListenableFuture<T> call(AsyncExecutionContext<T> context, ListenableFuture<T> data)
			throws ExecutionException;
//	public ListenableFuture<T> callItem(AsyncExecutionContext<T> context, ListenableFuture<T> data)
//			throws ExecutionException;
}
