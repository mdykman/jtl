package org.dykman.jtl.core.engine;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractInstruction<T> implements Instruction<T> {

	List<AbstractInstruction<T>> list = new ArrayList<>();
	@Override
	
	public List<T> callChildren(Engine<T> eng,T t) {
		List<T> res = new ArrayList<>();
		for(Instruction<T> in:list) {
			res.add(in.call(eng, t, null));
		}
		return res;
	}
	public void addChild(AbstractInstruction<T> c) {
		list.add(c);
	}
}
