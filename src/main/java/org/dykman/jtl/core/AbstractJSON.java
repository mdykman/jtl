package org.dykman.jtl.core;

public class AbstractJSON implements JSON {
	JSON parent;
	String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String s) {
		name = s;
	}

	@Override
	public JSON getParent() {
		return parent;
	}

	@Override
	public JSON setParent(JSON p) {
		return parent = p;
	}


}
