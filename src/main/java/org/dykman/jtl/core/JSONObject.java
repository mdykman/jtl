package org.dykman.jtl.core;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
	protected boolean isKeySafe(String k) {
		int n = k.length();
		for (int i = 0; i < n; ++i) {
			Character cc = k.charAt(i);
			if (!(Character.isAlphabetic(cc) || Character.isDigit(cc) || cc
					.equals('_'))) {
				return false;
			}
		}
		return true;

	}

	@Override
	public void write(Writer out, int indent) throws IOException {
		write(out, indent, 0);
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
	public boolean isFalse() {
		return obj.size() == 0;
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
		if (locked)
			raise("container is locked");
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
}
