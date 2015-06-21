package org.dykman.jtl.core;

import java.util.Collection;

import org.dykman.jtl.core.JSON.JSONType;

public class Frame extends JSONArray {

	public Frame(Collection<JSON> col) {
		super(null,col);
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

}
