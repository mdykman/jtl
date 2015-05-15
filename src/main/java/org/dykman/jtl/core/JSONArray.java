package org.dykman.jtl.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.dykman.jtl.core.engine.CollectionFactory;

public class JSONArray extends AbstractJSON implements Iterable<JSON> {
	private Collection<JSON> arr = new ArrayList<>();

	private ArrayList<JSON> theList = null;
/*	public JSONArray(JSON parent) { 
		super(parent); 
		arr= new ArrayList<>();
	}
	*/
	protected JSONArray(JSON parent,Collection< JSON> coll) {
		super(parent);
		arr = coll;
	}
	@Override
	public boolean isNull() {
		return arr.size() == 0;
	}

	public static JSONArray create(JSON parent,CollectionFactory<JSON> factory) {
		return new JSONArray(parent,factory.createCollection()); 
	}
	public void add(JSON j) {
		if(locked) raise("container is locked");
		arr.add(j);
	}

	public void addAll(Collection<JSON> j) {
		if(locked) raise("container is locked");

		arr.addAll(j);
	}
	public Boolean booleanValue() {
		return arr.size() != 0;
	}
	
	public ArrayList<JSON> asList() {
		if(arr instanceof ArrayList) {
			return (ArrayList<JSON>)arr;
		}
		return new ArrayList<>(arr);
	}

	public JSON get(int i) {
		if(theList == null) {
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

}
