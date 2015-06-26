package org.dykman.jtl.core.future;

import org.dykman.jtl.core.ExecutionException;


public interface DyadicAsyncFunction<T> {
	public T invoke(AsyncExecutionContext<T> eng,T a,T b)
		throws ExecutionException;
	
}