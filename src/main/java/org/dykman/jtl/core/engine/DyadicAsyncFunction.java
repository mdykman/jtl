package org.dykman.jtl.core.engine;

public interface DyadicAsyncFunction<T> {
	public T invoke(AsyncEngine<T> eng,T a,T b);
	
}