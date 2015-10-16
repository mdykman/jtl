package org.dykman.jtl.future;

import org.dykman.jtl.Pair;

public class InstructionFutureValue<T> {
	public FutureInstruction<T> inst;
//	public Duo<Instruction<T>,Instruction<T>> duo;
	public Pair<String,FutureInstruction<T>> ninst;
	
	public String string;
	public Number number;
	public InstructionFutureValue(String s) {
		string = s;
	}
   public InstructionFutureValue(Number s) {
      number = s;
   }
	public InstructionFutureValue(FutureInstruction<T> t) {
		inst = t;
	}
	/*
	public InstructionValue(Instruction<T> first,Instruction<T> second) {
		duo = new Duo<>(first,second);
	}
	*/
	public InstructionFutureValue(String first,FutureInstruction<T> second) {
		ninst = new Pair<>(first,second);
	}
}
