package org.dykman.jtl.core.engine;

import com.google.common.util.concurrent.AsyncFunction;

/*
	public static InstructionFuture<JSON> pair(final JSONObject object,final InstructionFuture<JSON>k,final InstructionFuture<JSON>v) {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(AsyncEngine<JSON>eng,ListenableFuture<JSON> t, List<ListenableFuture<JSON>> args) {
				k.call(eng, t, null);
				object.put(k.call(eng, t, null).toString(), v.call(eng, t, null));
				return object;
			}
		};
	}
*/ 
	public abstract class KeyedAsyncFunction<I,O,K> implements AsyncFunction<I, O> {
		K k;
		public KeyedAsyncFunction(K k) {this.k=k;}
	}