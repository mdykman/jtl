package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public interface InstructionFuture<T> {


	public InstructionFuture<T> unwrap(AsyncExecutionContext<T> context);
	public ListenableFuture<T> call(AsyncExecutionContext<T> context, ListenableFuture<T> data)
			throws ExecutionException;
}
