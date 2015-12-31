package org.dykman.jtl.future;

import java.util.List;

import org.dykman.jtl.Pair;
import org.dykman.jtl.operator.FutureInstruction;
import org.dykman.jtl.operator.ObjectInstructionBase.ObjectKey;

public class FutureInstructionValue<T> {
	public FutureInstruction<T> inst;
	public List<FutureInstruction<T>> linst;
//	public Duo<Instruction<T>,Instruction<T>> duo;
	public Pair<ObjectKey,FutureInstruction<T>> ninst;
	public ObjectKey key;
	
	public String string;
	public Number number;
	public FutureInstructionValue(String s) {
		string = s;
	}
	public FutureInstructionValue(ObjectKey k) {
		key = k;
	}
   public FutureInstructionValue(Number s) {
      number = s;
   }
	public FutureInstructionValue(FutureInstruction<T> t) {
		inst = t;
	}
	public FutureInstructionValue(List<FutureInstruction<T>> t) {
		linst = t;
	}
	/*
	public InstructionValue(Instruction<T> first,Instruction<T> second) {
		duo = new Duo<>(first,second);
	}
	*/
	public FutureInstructionValue(ObjectKey first,FutureInstruction<T> second) {
		ninst = new Pair<>(first,second);
	}
}
