package org.dykman.jtl.core.parser;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.engine.AsyncEngine;

public abstract class MathOp {

	public JSON op(AsyncEngine<JSON> eng,JSON l, JSON r) {
		Number ln = eng.number(l);
		Number rn = eng.number(r);
		if(ln!=null&&rn!=null) {
			return new JSONValue(null,op(eng,ln,rn));
		}
		if(l instanceof JSONValue) {
			JSONValue lv = (JSONValue) l;
			if(r instanceof JSONValue) {
				JSONValue rv = (JSONValue) r;
				Object ro = lv.get();
				Object lo = rv.get();
			}
		}
		return null;
	}

	public abstract long op(AsyncEngine<JSON> eng,long l, long r);

	public abstract String op(AsyncEngine<JSON> eng,String l, String r);

	public Number op(AsyncEngine<JSON> eng,Number l, Number r) {
		if (l instanceof Long && r instanceof Long) {
			return op(eng,l.longValue(), r.longValue());
		}
		return op(eng,l.doubleValue(), r.doubleValue());
	}

}