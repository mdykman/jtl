package org.dykman.jtl.core.engine.future;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


public abstract class AbstractInstructionFuture<T> implements InstructionFuture<T> {

	static Cache<String, Pattern> PatternCache = 
	CacheBuilder.newBuilder().initialCapacity(500).expireAfterAccess(5000, TimeUnit.SECONDS).build();


//	List<AbstractInstructionFuture<T>> list = new ArrayList<>();
//	@Override
	/*
	public ListenableFuture<List<T>> callChildren(ListenableFuture<T> t) {
		List<T> res = new ArrayList<>();
		for(Instruction<T> in:list) {
			res.add(in.call(eng, t, null));
		}
		return res;
	}
	*/
}
