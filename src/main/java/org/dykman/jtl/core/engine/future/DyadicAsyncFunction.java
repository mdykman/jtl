package org.dykman.jtl.core.engine.future;

import org.dykman.jtl.core.engine.ExecutionException;


public interface DyadicAsyncFunction<T> {
	public T invoke(AsyncExecutionContext<T> eng,T a,T b)
		throws ExecutionException;
	
}