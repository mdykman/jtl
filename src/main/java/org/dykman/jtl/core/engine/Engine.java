package org.dykman.jtl.core.engine;

import java.util.List;

public interface Engine<T> extends MapFactory<String, T>, CollectionFactory<T> {
	public T execute(T c,Instruction<T> i);
	
	public void define(String n,Instruction<T> i);	
	public T call(String name,List<T> args);

	public void set(String name,T t);
	public T lookup(String name);
	
}
