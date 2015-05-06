package org.dykman.jtl.core.parser;

import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.engine.Instruction;

public class InstructionValue<T> {
	public Instruction<T> inst;
//	public Duo<Instruction<T>,Instruction<T>> duo;
	public Duo<String,Instruction<T>> ninst;
	
	public InstructionValue(Instruction<T> t) {
		inst = t;
	}
	/*
	public InstructionValue(Instruction<T> first,Instruction<T> second) {
		duo = new Duo<>(first,second);
	}
	*/
	public InstructionValue(String first,Instruction<T> second) {
		ninst = new Duo<>(first,second);
	}
}
