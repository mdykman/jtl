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

}
