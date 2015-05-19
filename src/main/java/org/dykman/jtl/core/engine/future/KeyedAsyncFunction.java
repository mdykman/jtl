package org.dykman.jtl.core.engine.future;

import com.google.common.util.concurrent.AsyncFunction;

public abstract class KeyedAsyncFunction<I, O, K> implements
		AsyncFunction<I, O> {
	K k;

	public KeyedAsyncFunction(K k) {
		this.k = k;
	}
}