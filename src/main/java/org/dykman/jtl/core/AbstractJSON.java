package org.dykman.jtl.core;

public abstract class AbstractJSON implements JSON {
	JSON parent;
	String name;
	boolean locked = false;

	public AbstractJSON(JSON parent) { this.parent = parent; }
	public void raise(String msg) {
		throw new RuntimeException(msg);
	}
	@Override public void lock() {
		locked=true;
	}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String s) {
		if(locked) raise("container is locked");
		name = s;
	}

	@Override
	public JSON getParent() {
		return parent;
	}
	
	

	@Override
	public JSON setParent(JSON p) {
		if(locked) raise("container is locked");
		return parent = p;
	}

	public abstract boolean isNull();
}
