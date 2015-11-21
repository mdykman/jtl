package org.dykman.jtl.operator;

import com.google.common.util.concurrent.AsyncFunction;

public abstract class KeyedAsyncFunction<I, O, K> implements
		AsyncFunction<I, O> {
	public K k;

	public KeyedAsyncFunction(K k) {
		this.k = k;
	}
}