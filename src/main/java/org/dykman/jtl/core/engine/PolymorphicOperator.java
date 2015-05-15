package org.dykman.jtl.core.engine;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.JSON.JSONType;

public abstract class PolymorphicOperator {

	public JSON op(AsyncEngine<JSON> eng, JSON l, JSON r) 
		throws JSONException {
		switch(l.getType()) {
		case ARRAY:
			switch(r.getType()) {
			case ARRAY:
					return op(eng,(JSONArray)l,(JSONArray)r);
			case BOOLEAN:
			case DOUBLE:
			case LONG:
			case NULL:
			case OBJECT:
			case STRING:
			}
			break;
		case OBJECT:
			switch (r.getType()) {
			case OBJECT:
				return op(eng,(JSONObject)l,(JSONObject)r);
			case ARRAY:
			case BOOLEAN:
			case DOUBLE:
			case LONG:
			case NULL:
			case STRING:
			}
			break;
		case BOOLEAN:
		case DOUBLE:
		case LONG:
		case NULL:
		case STRING:
		}
		throw new JSONException("not imlemented yet");
//		return new JSONValue(null);

	}
	public JSON __op(AsyncEngine<JSON> eng, JSON l, JSON r) {
		if (l instanceof JSONValue) {
			if(((JSONValue) l).isNull()) {
				return new JSONValue(null);// any operation with a null, results in a null;
			}
			Number rn = null;
			String rs = null;
			// left boolean
			if (eng.is_boolean(l)) {
				if (eng.is_boolean(r)) {
					return new JSONValue(null,
							op(eng, eng.bool(r), eng.bool(l)));
				}
				rn = eng.number(r);
				if (rn != null) {
					if (rn instanceof Long) {
						return new JSONValue(null, op(eng, eng.bool(l) ? 1L
								: 0L, rn.longValue()));
					} else {
						return new JSONValue(null, op(eng,
								(double) (eng.bool(l) ? 1.0f : 0.0f),
								rn.doubleValue()));
					}
				}
				return new JSONValue(null,
						op(eng, eng.bool(r), eng.bool(l)));
			}
			String ls =null;
			Number ln = eng.number(l);
			rn = rn == null ? eng.number(r) : rn;
			// left numeric
			if (ln != null) {
				if (rn != null) {
					if (ln instanceof Long && rn instanceof Long) {
						return new JSONValue(null, op(eng, ln.longValue(),
								rn.longValue()));
					} else {
						return new JSONValue(null, op(eng, ln.doubleValue(),
								rn.doubleValue()));
					}
				}
				rs = rs == null ? eng.is_string(r) : rs;
				try {
					rn = Long.parseLong(rs);
					return new JSONValue(null, op(eng, ln.longValue(),
							rn.longValue()));
				} catch(NumberFormatException e) {
					try {
						ln = Double.parseDouble(ls);
						return new JSONValue(null, op(eng, ln.doubleValue(),
								rn.doubleValue()));
					} catch(NumberFormatException ee) {
						return new JSONValue(null);
					}
				}
			}
			
			ls = ls == null ? eng.is_string(l) : ls;
			rs = rs == null ? eng.is_string(r) : rs;
			if(ls != null) {
				return new JSONValue(null, op(eng, ls,rs));
				 
			}
		}
		if(l instanceof JSONObject && r instanceof JSONObject) {
			return op(eng, (JSONObject)l,
					(JSONObject)r);
			
		}
		if(l instanceof JSONArray && r instanceof JSONArray) {
			return op(eng, (JSONArray)l,
					(JSONArray)r);
			
		}
		return new JSONValue(null);
		
	}

	
	protected abstract JSON op(AsyncEngine<JSON> eng, JSONArray l, JSONArray r);


	protected abstract JSON op(AsyncEngine<JSON> eng, JSONObject l, JSONObject r);


	protected abstract Boolean op(AsyncEngine<JSON> eng, Boolean l, Boolean r);

	protected abstract Long op(AsyncEngine<JSON> eng, Long l, Long r);

	protected abstract Double op(AsyncEngine<JSON> eng, Double l, Double r);

	protected abstract String op(AsyncEngine<JSON> eng, String l, String r);
}