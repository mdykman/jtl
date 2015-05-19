package org.dykman.jtl.core.engine.future;

import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.engine.Instruction;

public class InstructionFutureValue<T> {
	public InstructionFuture<T> inst;
//	public Duo<Instruction<T>,Instruction<T>> duo;
	public Duo<String,InstructionFuture<T>> ninst;
	
	public InstructionFutureValue(InstructionFuture<T> t) {
		inst = t;
	}
	/*
	public InstructionValue(Instruction<T> first,Instruction<T> second) {
		duo = new Duo<>(first,second);
	}
	*/
	public InstructionFutureValue(String first,InstructionFuture<T> second) {
		ninst = new Duo<>(first,second);
	}
}
