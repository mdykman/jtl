package org.dykman.jtl.ext;

import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.ArithmaticPolymophicOperator;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JList;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;

public class MathExt {

	JSONObject global;
	JSONObject module;
	SourceInfo si = SourceInfo.internal("math");
	interface Monadic {
		Double op(Double d);
	}
	
	public MathExt(JSONObject g, JSONObject m) {
		global = g;
		module = m;
	}
	

	@JtlMethod(name="sqrt")
	public JSON sqrt(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params) 
		throws ExecutionException {
		return monadic(context,data,params,
				d -> Math.sqrt(d));
	}

	@JtlMethod(name="sq")
	public JSON sq(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params) 
		throws ExecutionException {
		return monadic(context,data,params,
				d -> d*d);
	}

	@JtlMethod(name="sin")
	public JSON sin(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params) 
		throws ExecutionException {
		return monadic(context,data,params,
				d -> Math.sin(d));
	}
	
	@JtlMethod(name="cos")
	public JSON cos(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params) 
		throws ExecutionException {
		return monadic(context,data,params,
				d -> Math.cos(d));
	}
	
	@JtlMethod(name="tan")
	public JSON tan(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params) 
		throws ExecutionException {
		return monadic(context,data,params,
				d -> Math.tan(d));
	}
	
	@JtlMethod(name="log")
	public JSON log(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params) 
		throws ExecutionException {
		return monadic(context,data,params,
				d -> Math.log(d));
	}
	

	
	@JtlMethod(name="pow")
	public JSON pow(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params) 
		throws ExecutionException {
		return dyadic(context,data,params, new ArithmaticPolymophicOperator(si){

			@Override
			public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
				return Math.pow(l, r);
			}
		});
	}
	
	protected JSON monadic(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params, Monadic monadic) 
			throws ExecutionException {
		JSON f;
		if(params.size() > 0) {
			f = params.get(0);
		} else {
			f = data;
		}	
		if(f.getType() == JSONType.NULL) {
			return context.builder().value();
		}
		if(f instanceof JSONArray) {
			JSONArray array = 
					data instanceof JList ? 
					context.builder().list() : context.builder().array(null);
			for(JSON j: (JSONArray)f) {
				if(j instanceof JSONValue) {
					Double d = ((JSONValue)j).doubleValue();
					array.add(context.builder().value(monadic.op(d)));
				}
			}
			return array;
		} else if (f instanceof JSONValue) {
			Double d = ((JSONValue)f).doubleValue();
			return context.builder().value(monadic.op(d));
		}
		return context.builder().value();
	}
	protected JSON dyadic(AsyncExecutionContext<JSON> context,JSON data,List<JSON> params, 
			ArithmaticPolymophicOperator dyad) 
		throws ExecutionException {
		JSON f;
		JSON s;
		if(params.size() > 0) {
			f = params.get(0);
			s = params.get(1);
		} else {
			f = data;
			s = params.get(0);
		}	
		return dyad.op(context, f, s);
	}
}