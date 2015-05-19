package org.dykman.jtl.core.engine.future;


public abstract class AbstractInstructionFuture<T> implements InstructionFuture<T> {


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
