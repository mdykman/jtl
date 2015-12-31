package org.dykman.jtl.json;

import org.dykman.jtl.Pair;

public class DataValue<T> {

	public T value;
	public String str;
	public Pair<String, T> pair;
	
	public DataValue(String s) { str = s; }
	public DataValue(T value) {
		this.value = value;
	}
	public DataValue(Pair<String, T> pair) {
		this.pair = pair;
	}

}
