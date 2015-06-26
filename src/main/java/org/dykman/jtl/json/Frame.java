package org.dykman.jtl.json;

import java.util.Collection;

import org.dykman.jtl.json.JSON.JSONType;

public class Frame extends JSONArray {

	public Frame(Collection<JSON> col) {
		super(null,col);
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
