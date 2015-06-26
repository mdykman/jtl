package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;


public interface DyadicAsyncFunction<T> {
	public T invoke(AsyncExecutionContext<T> eng,T a,T b)
		throws ExecutionException;
	
}