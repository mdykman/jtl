package org.dykman.jtl.json;

import java.util.Collection;

public class Frame extends JSONArray {

	public Frame(Collection<JSON> col) {
		this(null,col);
	}
	public Frame(JSON parent,Collection<JSON> col) {
		super(parent,col);
		hash = 45763478;
	}
	@Override
	public JSONType getType() {
		return JSONType.FRAME;
	}

	public JSON cloneJSON() {
		Frame res = builder.frame();
		int i = 0;
		for(JSON j:arr) {
			res.add(j);
		}
		return res;
	}

	public void add(JSON j) {
		add(j,false);
	}
	public void add(JSON j,boolean dontclone) {
		hash ^= j.hashCode();
		arr.add(j);
	}

}
