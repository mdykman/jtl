package org.dykman.jtl.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import org.apache.commons.lang3.StringEscapeUtils;

public abstract class AbstractJSON implements JSON {
	JSON parent;
	String name;
	boolean locked = false;
	Integer index = null;
	static final int charbuffer = 8192;
	
	static final char[] SPACES = new char[charbuffer];
	static {
		Arrays.fill(SPACES, ' ');
	}

	public AbstractJSON(JSON parent) { this.parent = parent; }
	public void raise(String msg) {
		throw new RuntimeException(msg);
	}
	protected void writeAsString(Writer writer,String s) 
		throws IOException {
		writer.write('"');
		StringEscapeUtils.escapeJson(s);
		writer.write('"');

	}
	protected void indent(Writer writer,int indent,int depth)
		throws IOException {
		int n = indent * depth;
		writer.write(SPACES, 0, n);
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
	@Override
	public void write(Writer out, int indent) throws IOException {
		write(out,indent,0);
	}

	public abstract boolean isFalse();
	public Integer getIndex() {
		return index;
	}
	public void setIndex(Integer index) {
		this.index = index;
	}
}
