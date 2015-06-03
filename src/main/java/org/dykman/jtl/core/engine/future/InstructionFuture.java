package org.dykman.jtl.core.engine.future;

import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

public interface InstructionFuture<T> {

	public ListenableFuture<T> call(AsyncExecutionContext<T> context, ListenableFuture<T> data)
		throws ExecutionException;
}
