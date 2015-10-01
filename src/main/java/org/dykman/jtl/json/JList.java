package org.dykman.jtl.json;

import java.util.Collection;

public class JList extends JSONArray {

	public JList(Collection<JSON> col) {
		this(null,col);
	}
	public JList(JSON parent,Collection<JSON> col) {
		super(parent,col);
		hash = 45763478;
	}
	@Override
	public JSONType getType() {
		return JSONType.LIST;
	}

	public JSON cloneJSON() {
		JList res = builder.frame();
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
