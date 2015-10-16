package org.dykman.jtl.future;

import org.dykman.jtl.Pair;

public class FutureInstructionValue<T> {
	public FutureInstruction<T> inst;
//	public Duo<Instruction<T>,Instruction<T>> duo;
	public Pair<String,FutureInstruction<T>> ninst;
	
	public String string;
	public Number number;
	public FutureInstructionValue(String s) {
		string = s;
	}
   public FutureInstructionValue(Number s) {
      number = s;
   }
	public FutureInstructionValue(FutureInstruction<T> t) {
		inst = t;
	}
	/*
	public InstructionValue(Instruction<T> first,Instruction<T> second) {
		duo = new Duo<>(first,second);
	}
	*/
	public FutureInstructionValue(String first,FutureInstruction<T> second) {
		ninst = new Pair<>(first,second);
	}
}
