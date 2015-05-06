package org.dykman.jtl.core.engine;

import com.google.common.util.concurrent.AsyncFunction;

public abstract class KeyedPayloadAsyncFunction<I,O,K,P> implements AsyncFunction<I, O> {
	K k; P p;
	public KeyedPayloadAsyncFunction(K k,P p) {this.k=k;this.p=p;}
}