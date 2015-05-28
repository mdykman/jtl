package org.dykman.jtl.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.dykman.jtl.core.JSONBuilder;

public abstract class AbstractInstruction<T> implements Instruction<T> {

	List<AbstractInstruction<T>> list = new ArrayList<>();
	JSONBuilder builder;
	
	
	public AbstractInstruction(JSONBuilder builder) {
		this.builder = builder;
	}
	public List<T> callChildren(Engine<T> eng,T t) {
		List<T> res = new ArrayList<>();
		for(Instruction<T> in:list) {
			res.add(in.call(eng, t, null));
		}
		return res;
	}
	/*
	@Override
	public void addChild(AbstractInstruction<T> c) {
		list.add(c);
		
		ConcurrentLinkedQueue<String> j = new ConcurrentLinkedQueue<>();
//		j.
	}
	*/
}