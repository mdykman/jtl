package org.dykman.jtl.core.engine;

import java.util.List;

public interface Instruction<T> {

	public T call(Engine<T> eng,T t,List<T>args);
	public List<T> callChildren(Engine<T> eng,T t);
}
