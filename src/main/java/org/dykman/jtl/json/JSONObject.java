package org.dykman.jtl.json;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.dykman.jtl.Pair;

public class JSONObject extends AbstractJSON implements
		Iterable<Pair<String, JSON>> {
	final Map<String, JSON> obj;
	int hash = 12345678;

	public JSONObject(JSON parent, Map<String, JSON> map) {
		super(parent);
		obj = map;
	}

	public Map<String, JSON> map() {
		return obj;
	}
	
	public JSONObject overlay(JSONObject rhs) {
	   
	   JSONObject newone = builder.object(getParent());
	   Set<String> seen = new HashSet<>();
	   for(Pair<String,JSON> pp: this) {
	      String k = pp.f;
	      JSON j = pp.s;
	      if(rhs.containsKey(k)) {
	         seen.add(k);
	         JSON other = rhs.get(k);
	         // maps can merge
	         if(j instanceof JSONObject && other instanceof JSONObject) {
	            newone.put(k, ((JSONObject)j).overlay((JSONObject)other));
	         } else {
	            // otherwise, RHS overrides
	            newone.put(k, other);
	         }
	      } else {
	         newone.put(k, j);
	      }
	   }
      for(Pair<String,JSON> pp: rhs) {
         if(!seen.contains(pp.f)) {
            newone.put(pp.f, pp.s);
         }
      }	   
	   return newone;
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
			int rr = lj.getValue().compareTo(rvj);
			if(rr!=0) return rr;
		}
		return 0;
	}


	@Override
	public void write(Writer out, int n, int d,boolean fq) throws IOException {
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
			if (fq == false && isKeySafe(k)) {
				out.write(k);
			} else {
				writeAsString(out, k);
			}
			out.write(':');
			if (n > 0) {
				out.write(' ');
			}
			ee.getValue().write(out, n, d +1,fq);
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
/*
	public static JSONObject create(JSON parent, MapFactory factory)
			throws JSONException {
		return new JSONObject(parent, factory.createMap());
	}
*/
	public void put(String k, JSON v) {
		put(k,v,false);
	}
	public void put(String k, JSON v, boolean dontclone) {
		if (locked)
			raise("container is locked");
		if(!dontclone) v = v.cloneJSON();
		hash ^= k.hashCode();
		hash ^= v.hashCode();
		v.setParent(this);
		v.setName(k);
		v.lock();
		obj.put(k, v);
	}
	public int hashCode() {
		return hash;
	}

	public void putAll(JSONObject v) {
		if (locked)
			raise("container is locked");
		for(Pair<String, JSON> pp : v) {
			put(pp.f,pp.s);
		}
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
//		JSONObject obj = builder.object(parent);
		JSONObject obj = builder.object(null, builder.map(this.obj));
		/*
		Iterator<Map.Entry<String, JSON>> it = this.obj.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String,JSON> ee = it.next();
			obj.put(ee.getKey(), ee.getValue());
		}
		*/
		return obj;
	}
}
