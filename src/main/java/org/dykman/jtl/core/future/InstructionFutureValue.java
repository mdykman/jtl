package org.dykman.jtl.core.future;

import org.dykman.jtl.core.Pair;

public class InstructionFutureValue<T> {
	public InstructionFuture<T> inst;
//	public Duo<Instruction<T>,Instruction<T>> duo;
	public Pair<String,InstructionFuture<T>> ninst;
	
	public String string;
	public InstructionFutureValue(String s) {
		string = s;
	}
	public InstructionFutureValue(InstructionFuture<T> t) {
		inst = t;
	}
	/*
	public InstructionValue(Instruction<T> first,Instruction<T> second) {
		duo = new Duo<>(first,second);
	}
	*/
	public InstructionFutureValue(String first,InstructionFuture<T> second) {
		ninst = new Pair<>(first,second);
	}
}
