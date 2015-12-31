package org.dykman.jtl.operator;

import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.jsonVisitor;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONValue;

public abstract class ArithmaticPolymophicOperator extends DefaultPolymorphicOperator {
	public ArithmaticPolymophicOperator(SourceInfo source) {
		super(source);
	}
	@Override
	public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
		return op(eng,l.doubleValue(),r.doubleValue()).longValue();
	}

	@Override
	public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
		if(!r.isNumber()) return eng.builder().array(null);
		Number n = (Number)((JSONValue) r).get();
		return op(eng,l,n);
	}
	@Override
	public JSONArray op(AsyncExecutionContext<JSON> eng, JSON l, JSONArray r) {
		if(!r.isNumber()) return eng.builder().array(null);
		Number n = (Number)((JSONValue) l).get();
		return op(eng,n,r);
	}
	
	public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, Number r) {
		JSONArray arr = builder.array(null);
		for (JSON j : l.collection()) {
			if(j instanceof JSONValue) {
				JSONValue jval = (JSONValue) j;
				Object o = jval.get();
				if(o == null || jval.getType() == JSONType.NULL) {
					arr.add(builder.value(op(eng,JSONBuilderImpl.NULL,r)));
				} else if(o instanceof Number) {
					arr.add(builder.value(op(eng,(Number)o,r)));
				} else if(o instanceof Boolean) {
					arr.add(builder.value(op(eng,(Boolean)o ? 1L : 0L ,r)));
				} else {
					arr.add(builder.value());
				}
			}
		}
		return arr;
	}
	
	public JSONArray op(AsyncExecutionContext<JSON> eng, Number l, JSONArray r) {
		JSONArray arr = builder.array(null);
		for (JSON j : r) {
			if(j instanceof JSONValue) {
				JSONValue jval = (JSONValue) j;
				Object o = jval.get();
				if(o == null || jval.getType() == JSONType.NULL) {
					arr.add(builder.value(op(eng,l,JSONBuilderImpl.NULL)));
				} else if(o instanceof Number) {
					arr.add(builder.value(op(eng,l,(Number)o)));
				} else if(o instanceof Boolean) {
					arr.add(builder.value(op(eng,l,(Boolean)o ? 1L : 0L )));
				} else {
					arr.add(builder.value());
				}
			}
		}
		return arr;
	}
	
	protected JSON op(AsyncExecutionContext<JSON> context, JSON l, Number r) {
		return op(context,r,l);
	}	
	protected JSON op(AsyncExecutionContext<JSON> context, Number l, JSON r) {
		if(r instanceof JSONArray) {
			return op(context,l,(JSONArray) r);
		} else {
		return JSONBuilderImpl.NULL;
		}
	}

	protected Number op(AsyncExecutionContext<JSON> context, Number l, Number r) {
		if(l instanceof Double || r instanceof Double) {
			return op(context,l.doubleValue(),r.doubleValue());
		}
		return op(context,l.longValue(),r.longValue());
	}
	
	public abstract Double op(AsyncExecutionContext<JSON> eng, Double l, Double r);
}