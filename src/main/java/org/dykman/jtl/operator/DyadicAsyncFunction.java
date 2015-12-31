package org.dykman.jtl.operator;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.future.AsyncExecutionContext;


public interface DyadicAsyncFunction<T> {
	public T invoke(AsyncExecutionContext<T> eng,T a,T b)
		throws ExecutionException;
	
}