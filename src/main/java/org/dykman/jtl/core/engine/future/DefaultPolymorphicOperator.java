package org.dykman.jtl.core.engine.future;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.engine.ExecutionException;

public class DefaultPolymorphicOperator implements PolymorphicOperator, DyadicAsyncFunction<JSON> {

	

	@Override
	public JSON invoke(AsyncExecutionContext<JSON> eng, JSON a, JSON b) 
			throws ExecutionException {
		return op(eng,a,b);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> eng, JSON l, JSON r) throws ExecutionException {
		switch (l.getType()) {
		case ARRAY: {
			JSONArray left = (JSONArray) l;
			switch (r.getType()) {
			case ARRAY:
				return op(eng, left, (JSONArray) r);
			case OBJECT:
				return op(eng, left, (JSONObject) r);
			case BOOLEAN:
			case DOUBLE:
			case LONG:
			case NULL:
			case STRING:
				return op(eng, left, r);
			}
			break;
		}
		case OBJECT: {
			JSONObject left = (JSONObject) l;
			switch (r.getType()) {
			case OBJECT:
				return op(eng, left, (JSONObject) r);
			case ARRAY:
				return op(eng, left, (JSONArray) r);
			case BOOLEAN:
			case DOUBLE:
			case LONG:
			case NULL:
			case STRING:
				return op(eng,left, r);
			}
			break;
		}
		case BOOLEAN: {
			Boolean left = (Boolean) ((JSONValue) l).get();
			switch (r.getType()) {
			case BOOLEAN:
				return new JSONValue(null, op(eng,left,
						(Boolean) ((JSONValue) r).get()));
			case DOUBLE:
				return new JSONValue(null, op(eng, left ? (double)1f  : (double)0f,
						(Double) ((JSONValue) r).get()));
			case LONG:
				return new JSONValue(null, op(eng,left ? 1L  : 0L,
						(Long) ((JSONValue) r).get()));
			case OBJECT:
			case ARRAY:
			case NULL:
			case STRING:
				return  new JSONValue(null,op(eng, left,
						eng.engine().bool(r)));
			}
			break;
		}
		case LONG: {
			Long left  = (Long) ((JSONValue) l).get();
			switch (r.getType()) {
			case LONG:
				return new JSONValue(null, op(eng,left,
						(Long) ((JSONValue) r).get()));
			case DOUBLE:
				return new JSONValue(null, op(eng,left.doubleValue(),
						((Double) ((JSONValue) r).get())));
			case BOOLEAN:
				return new JSONValue(null, op(eng,left,
						(Boolean) ((JSONValue) r).get() ? 1L : 0L));
			case STRING:
				return new JSONValue(null, op(eng,left.toString(),
						((JSONValue) r).get().toString()));
			case NULL:
				return new JSONValue(null);
			case ARRAY:
			case OBJECT:
				return op(eng,left,r);
			}
			break;
		}
		case DOUBLE: {
			Double left = ((Double) ((JSONValue) l).get());
			switch (r.getType()) {
			case DOUBLE:
				return new JSONValue(null, op(eng,left,
						((Double) ((JSONValue) r).get())));
			case LONG:
				return new JSONValue(null, op(eng,left,
						((Long) ((JSONValue) r).get()).doubleValue()));
			case BOOLEAN:
				return new JSONValue(null, op(eng, left,
						(Boolean) ((JSONValue) r).get() ? (double)1.0f : 0.0f));
			case STRING:
				return new JSONValue(null, op(eng,left.toString(),
						((JSONValue) r).get().toString()));
			case NULL:
//				return new JSONValue(null);
			case ARRAY:
			case OBJECT:
				return op(eng,left,r);
			}
			break;
		}
		case STRING: {
			String left = ((JSONValue) l).get().toString();
			switch(r.getType()) {
			case DOUBLE:
			case LONG:
			case STRING:

				return new JSONValue(null, op(eng,left,
						((JSONValue) r).get().toString()));
			case NULL:
				return r;
			case ARRAY:
			case BOOLEAN:
			case OBJECT:
				return op(eng,left,r);
			}
			break;
		}
		case NULL:
			// TODO:: anything with this.
			return new JSONValue(null);
		}
		throw new ExecutionException("not implemented yet");
		// return new JSONValue(null);

	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONArray r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONObject r) {
		return new JSONValue(null);
	}


	@Override
	public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {
		return null;
	}

	@Override
	public Boolean op(AsyncExecutionContext<JSON> eng, Boolean l, Boolean r) {
		return null;
	}


	@Override
	public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {
		return null;
	}

	@Override
	public String op(AsyncExecutionContext<JSON> eng, String l, String r) {
		return null;
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONArray r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONObject r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> eng, JSONObject l, JSON r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> context, Boolean l, JSON r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> context, Long l, JSON r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> context, Double l, JSON r) {
		return new JSONValue(null);
	}

	@Override
	public JSON op(AsyncExecutionContext<JSON> context, String l, JSON r) {
		return new JSONValue(null);
	}

}
