package org.dykman.jtl.core;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JSONObject extends AbstractJSON implements Iterable<JSONObject.Pair> {
	Map<String, JSON> obj = new ConcurrentHashMap<>();
	public void put(String k,JSON v) {
		obj.put(k, v);
	}
	
	
	public Iterator<Pair> iterator() {
		final Iterator<Map.Entry<String, JSON>> it = obj.entrySet().iterator();
		return new Iterator<JSONObject.Pair>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Pair next() {
				Map.Entry<String, JSON> e = it.next();
				return new Pair(e.getKey(),e.getValue());
			}
		};
	}
	
	public JSON get(String k) {
		return obj.get(k);
	}
	public class Pair {
		String k;
		JSON v;
		public Pair(String k, JSON v) {
			this.k=k;
			this.v=v;
		}
	}
}
