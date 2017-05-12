package org.dykman.jtl.operator;

import org.dykman.jtl.json.JSON;

public class FunctionReference {
	final String name;
	final FutureInstruction<JSON> instruction;

	public FunctionReference(String name) {
		this.name = name;
		this.instruction = null;
	}
	public FunctionReference(FutureInstruction<JSON> instruction) {
		this.name = null;
		this.instruction = instruction;
	}

}
