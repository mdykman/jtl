package org.dykman.jtl.core;

public class JSONValue extends AbstractJSON {
	final Object o;

	public JSONValue(JSON p, Object o) {
		this.o = o;
		setParent(p);
	}

	public Object get() {
		return o;
	}

}
