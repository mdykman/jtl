package org.dykman.jtl.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.dykman.jtl.factory.CollectionFactory;

public class JSONArray extends AbstractJSON implements Iterable<JSON> {
	protected Collection<JSON> arr = new ArrayList<>();

	int hash = 45860934;
	private ArrayList<JSON> theList = null;
	private boolean bound = true;
	
	private boolean ranged = false;
	
	public boolean isRanged() {
		return ranged;
	}
	public void setRanged(boolean ranged) {
		this.ranged = ranged;
	}
	public JSONArray(JSON parent, Collection<JSON> coll) {
		this(parent,coll,true);
	}
	public JSONArray(JSON parent, Collection<JSON> coll,boolean bound) {
		super(parent);
		this.bound = bound;
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
			int rc = j.compareTo(rj);
			if(rc != 0) return rc;
		}
		return 0;
	}
	public JSON cloneJSON() {
		return builder.array(null, builder.collection(this.arr));
		/*
		JSONArray res = builder.array(parent);
		int i = 0;
		for(JSON j:arr) {
			res.add(j);
		}
		return res;
		*/
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
		if(bound) {
			if(!dontclone) j = j.cloneJSON();
			j.setParent(this);
			j.setIndex(arr.size());
			j.lock();
		}
		hash ^= j.hashCode();
		arr.add(j);
	}

	@Override public int hashCode() { return hash; }
	public void addAll(JSONArray a) {
		if (locked)
			raise("container is locked");

		for(JSON j : a) {
			add(j);
		}
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
		if(arr.size() == 0) return null;
		// let negatives reference from the end
		i=(i+theList.size()) % theList.size(); 
		return theList.get(i);
	}

	public Iterator<JSON> iterator() {
		return arr.iterator();
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
		if (j.getType() == JSONType.ARRAY || j.getType() == JSONType.LIST) {
			return true;
		}
		return false;
	}

	@Override
	public void write(Writer out, int indent, int depth,boolean fq) throws IOException {
		out.write('[');

		boolean first = true;
		JSONType ctype = firstChildType();
		if (ctype != JSONType.ARRAY && ctype != JSONType.LIST) {
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
				j.write(out, indent, depth + (ctype != JSONType.OBJECT ? 1 : 0),fq);
			}
		} else {
			for (JSON j : arr) {
				if (first) {
					first = false;
//					if(indent>0) out.write(' ');
				} else {
					out.write(',');
					if(indent > 0) out.write('\n');
					indent(out,indent,depth+1);
				}
				j.write(out, indent, depth + 1,fq);
			}

		}

		if (indent > 0 && ctype != JSONType.ARRAY && ctype != JSONType.LIST && ctype!=JSONType.OBJECT)
			out.write(' ');
		out.write(']');
	}

}
