package org.dykman.jtl.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.engine.MapFactory;

public class JSONObject extends AbstractJSON implements
		Iterable<Pair<String, JSON>> {
	Map<String, JSON> obj;

	public JSONObject(JSON parent) {
		super(parent);
		obj = new ConcurrentHashMap<>();
	}

	public JSONObject(JSON parent, Map<String, JSON> map) {
		super(parent);
		obj = map;
	}

	public void setMap(Map<String, JSON> obj) {
		this.obj = obj;
	}
	
	public Map<String, JSON> map() {
		return obj;
	}
	public boolean containsKey(String k) {
		return obj.containsKey(k);
	}
	protected boolean isKeySafe(String k) {
		int n = k.length();
		for (int i = 0; i < n; ++i) {
			Character cc = k.charAt(i);
			if(i==0 && Character.isDigit(cc)) return false;
			if (!(Character.isAlphabetic(cc) || Character.isDigit(cc) || cc
					.equals('_'))) {
				return false;
			}
		}
		return true;
	}
	public boolean equals(JSON r) {
		if(r == null) return false;
		if(r.getType()!= JSONType.OBJECT) return false;
		JSONObject rj = (JSONObject) r;
		return 0 == deepCompare(rj);
	}
	public int deepCompare(JSONObject r) {
		Map<String,JSON> rc = r.map();
		if(obj.size() !=  rc.size()) return obj.size() - r.size();
		for(Map.Entry<String, JSON> lj: obj.entrySet()) {
			String k = lj.getKey();
			JSON rvj = r.get(k);
			int rr = lj.getValue().compare(rvj);
			if(rr!=0) return rr;
		}
		return 0;
	}


	@Override
	public void write(Writer out, int n, int d) throws IOException {
		out.write('{');
		// if(n > 0) {
		// out.write('\n');
		// }
		boolean first = true;
		for (Map.Entry<String, JSON> ee : obj.entrySet()) {
			if (first) {
				first = false;
			} else {
				out.write(',');
			}
			if (n > 0) {
				out.write('\n');
				indent(out,n,d+1);
//				int nn = n * (1 + d);
//				for (int j = 0; j < nn; ++j) {
//					out.write(' ');
//				}
			}
			String k = ee.getKey();
			if (isKeySafe(k)) {
				out.write(k);
			} else {
				writeAsString(out, k);
			}
			out.write(':');
			if (n > 0) {
				out.write(' ');
			}
			ee.getValue().write(out, n, d +1);
		}
		if (n > 0) {
			out.write('\n');
			indent(out,n,d);
		}
		out.write('}');
	}

	@Override
	public boolean isTrue() {
		return obj.size() > 0;
	}

	@Override
	public JSONType getType() {
		return JSONType.OBJECT;
	}

	public static JSONObject create(JSON parent, MapFactory factory)
			throws JSONException {
		return new JSONObject(parent, factory.createMap());
	}
	public void put(String k, JSON v) {
		put(k,v,false);
	}
	public void put(String k, JSON v, boolean dontclone) {
		if (locked)
			raise("container is locked");
		if(!dontclone) v = v.cloneJSON();
		v.setParent(this);
		v.setName(k);
		lock();
		obj.put(k, v);
	}

	public void putAll(JSONObject v) {
		if (locked)
			raise("container is locked");
		obj.putAll(v.obj);
	}

	public Iterator<Pair<String, JSON>> iterator() {
		return new Iterator<Pair<String, JSON>>() {
			final Iterator<Map.Entry<String, JSON>> it = obj.entrySet()
					.iterator();

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Pair<String, JSON> next() {
				Map.Entry<String, JSON> e = it.next();
				return new Pair<>(e.getKey(), e.getValue());
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
	public JSON cloneJSON() {
		Map<String,JSON> m = builder.map();
		JSONObject obj = builder.object(null, m);
		for(Pair<String,JSON> ee :obj) {
			String k = ee.f;
			JSON j = ee.s.cloneJSON();
			j.setParent(obj);
			j.setName(k);
			j.lock();
			m.put(k, j);
		}
		obj.lock();
		return obj;
	}
}
