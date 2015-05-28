package org.dykman.jtl.core;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.dykman.jtl.core.engine.CollectionFactory;

public class JSONArray extends AbstractJSON implements Iterable<JSON> {
	private Collection<JSON> arr = new ArrayList<>();

	private ArrayList<JSON> theList = null;

	/*
	 * public JSONArray(JSON parent) { super(parent); arr= new ArrayList<>(); }
	 */
	public JSONArray(JSON parent, Collection<JSON> coll) {
		super(parent);
		arr = coll;
	}

	public boolean contains(JSON j) {
		return arr.contains(j);
	}
	public void setCollection(Collection<JSON> arr) {
		this.arr = arr;
	}
	public boolean equals(JSON r) {
		if(r == null) return false;
		if(r.getType()!= JSONType.ARRAY) return false;
		JSONArray rj = (JSONArray) r;
		return 0 == deepCompare(rj);
	}
	public int deepCompare(JSONArray r) {
		if(arr.size()!= r.size()) return arr.size() - r.size();
		Iterator<JSON> rit = r.iterator();
		for(JSON j: this) {
			JSON rj = rit.next();
			int rc = j.compare(rj);
			if(rc != 0) return rc;
		}
		return 0;
	}
	public JSON cloneJSON() {
		JSONArray res = builder.array(null);
		int i = 0;
		for(JSON j:arr) {
			res.add(j);
		}
		return res;
	}

	@Override
	public boolean isTrue() {
		return arr.size() > 0;
	}

	public Collection<JSON> collection() { return arr; }
	public static JSONArray create(JSON parent, CollectionFactory<JSON> factory) {
		return new JSONArray(parent, factory.createCollection());
	}
	public void add(JSON j) {
		add(j,false);
	}
	public void add(JSON j,boolean dontclone) {
		if (locked)
			raise("container is locked");
		if(!dontclone) j = j.cloneJSON();
		j.setParent(this);
		j.setIndex(arr.size());
		j.lock();
		arr.add(j);
	}

	public void addAll(Collection<JSON> j) {
		if (locked)
			raise("container is locked");

		arr.addAll(j);
	}

	public Boolean booleanValue() {
		return arr.size() != 0;
	}

	public ArrayList<JSON> asList() {
		if (arr instanceof ArrayList) {
			return (ArrayList<JSON>) arr;
		}
		return new ArrayList<>(arr);
	}

	public JSON get(int i) {
		if (theList == null) {
			theList = asList();
		}
		return theList.get(i);
	}

	public Iterator<JSON> iterator() {
		return arr.iterator();
	}

	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		boolean first = true;
		for (JSON j : arr) {
			if (first)
				first = false;
			else
				builder.append(",");
			builder.append(j.toString());
		}
		builder.append("]");
		return builder.toString();
	}

	public int size() {
		return arr.size();
	}

	@Override
	public JSONType getType() {
		return JSONType.ARRAY;
	}

	JSONType firstChildType() {
		if(arr.size() == 0) return null;
		return arr.iterator().next().getType();
	}
	boolean childIsArray() {
		if (arr.size() == 0)
			return false;
		JSON j = arr.iterator().next();
		if (j.getType() == JSONType.ARRAY) {
			return true;
		}
		return false;
	}

	@Override
	public void write(Writer out, int indent, int depth) throws IOException {
		out.write('[');

		boolean first = true;
		JSONType ctype = firstChildType();
//		boolean cia = type == JSONType.ARRAY;
		if (ctype != JSONType.ARRAY) {
			if (indent > 0 && ctype!=JSONType.OBJECT)
				out.write(' ');
			for (JSON j : arr) {
				if (first) {
					first = false;
				} else {
					out.write(',');
					if (indent > 0)
						out.write(' ');
				}
				j.write(out, indent, depth + (ctype != JSONType.OBJECT ? 1 : 0));
			}
		} else {
			for (JSON j : arr) {
				if (first) {
					first = false;
				} else {
					out.write(',');
					if(indent > 0) out.write('\n');
					indent(out,indent,depth+1);
				}
//				for (int k = 0; k < nn; ++k) {
//					out.write(' ');
//				}
				j.write(out, indent, depth + 1);
			}

		}

		if (indent > 0 && ctype!=JSONType.ARRAY && ctype!=JSONType.OBJECT)
			out.write(' ');
		out.write(']');
	}

}
