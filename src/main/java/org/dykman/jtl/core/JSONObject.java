package org.dykman.jtl.core;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dykman.jtl.core.engine.MapFactory;

public class JSONObject extends AbstractJSON implements Iterable<Pair<String,JSON>> {
	final Map<String, JSON> obj;
	
	public JSONObject(JSON parent) { 
		super(parent); 
		obj = new ConcurrentHashMap<>();
	}

	protected JSONObject(JSON parent, Map<String,JSON> map) { 
		super(parent); 
		obj = map;
	}
	@Override
	public boolean isNull() {
		return obj.size() == 0;
	}
	@Override
	public JSONType getType() {
		return JSONType.OBJECT;
	}

	public static JSONObject create(JSON parent,MapFactory factory) 
		throws JSONException {
			return new JSONObject(parent,factory.createMap());
	}
	public void put(String k,JSON v) {
		if(locked) raise("container is locked");
		obj.put(k, v);
	}
	
	public void putAll(JSONObject v) {
		if(locked) raise("container is locked");
		obj.putAll(v.obj);
	}
	
	public Iterator<Pair<String,JSON>> iterator() {
		return new Iterator<Pair<String,JSON>>() {
			final Iterator<Map.Entry<String, JSON>> it = obj.entrySet().iterator();
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}
			@Override
			public Pair<String,JSON> next() {
				Map.Entry<String, JSON> e = it.next();
				return new Pair<>(e.getKey(),e.getValue());
			}
		};
	}
	public Boolean booleanValue() {
		return obj.size() != 0;
	}
	
	public int size() {
		return obj.size();
	}
	public JSON get(String k) {
		return obj.get(k);
	}
}
