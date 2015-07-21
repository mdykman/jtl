package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;

public class DefaultPolymorphicOperator implements
// PolymorphicOperator,
	DyadicAsyncFunction<JSON> {
	JSONBuilder builder;

	public DefaultPolymorphicOperator() {
	}

	@Override
	public JSON invoke(AsyncExecutionContext<JSON> context, JSON a, JSON b) throws ExecutionException {
		return op(context, a, b);
	}

	// @Override
	public JSON op(AsyncExecutionContext<JSON> context, JSON l, JSON r) throws ExecutionException {
		builder = context.builder();
		JSONType ltype = l.getType();
		JSONType rtype = r.getType();
		if ((ltype == JSONType.ARRAY || ltype == JSONType.FRAME) && (rtype == JSONType.ARRAY || rtype == JSONType.FRAME))
			return op(context, (JSONArray) l, (JSONArray) r);

		switch (l.getType()) {
			case FRAME:
			case ARRAY: {
				JSONArray left = (JSONArray) l;
				switch (r.getType()) {
					case OBJECT:
						return op(context, left, (JSONObject) r);
					case BOOLEAN:
					case DOUBLE:
					case LONG:
					case NULL:
					case STRING:
						return op(context, left, r);
				}
				break;
			}
			case OBJECT: {
				JSONObject left = (JSONObject) l;
				switch (r.getType()) {
					case OBJECT:
						return op(context, left, (JSONObject) r);
					case FRAME:
					case ARRAY:
						return op(context, left, (JSONArray) r);
					case BOOLEAN:
					case DOUBLE:
					case LONG:
					case NULL:
					case STRING:
						return op(context, left, r);
				}
				break;
			}
			case BOOLEAN: {
				Boolean left = (Boolean) ((JSONValue) l).get();
				switch (r.getType()) {
					case BOOLEAN:
						return builder.value(op(context, left, (Boolean) ((JSONValue) r).get()));
					case DOUBLE:
						return builder.value(op(context, left ? (double) 1f : (double) 0f, (Double) ((JSONValue) r).get()));
					case LONG:
						return builder.value(op(context, left ? 1L : 0L, (Long) ((JSONValue) r).get()));
					case OBJECT:
					case ARRAY:
					case FRAME:
					case NULL:
					case STRING:
						return builder.value(op(context, left, r.isTrue()));
				}
				break;
			}
			case LONG: {
				Long left = (Long) ((JSONValue) l).get();
				switch (r.getType()) {
					case LONG:
						return builder.value(op(context, left, (Long) ((JSONValue) r).get()));
					case DOUBLE:
						return builder.value(op(context, left.doubleValue(), ((Double) ((JSONValue) r).get())));
					case BOOLEAN:
						return builder.value(op(context, left, (Boolean) ((JSONValue) r).get() ? 1L : 0L));
					case STRING:
						return builder.value(op(context, left.toString(), ((JSONValue) r).get().toString()));
					case NULL:
						return builder.value();
					case ARRAY:
					case FRAME:
					case OBJECT:
						return op(context, left, r);
				}
				break;
			}
			case DOUBLE: {
				Double left = ((Double) ((JSONValue) l).get());
				switch (r.getType()) {
					case DOUBLE:
						return builder.value(op(context, left, ((Double) ((JSONValue) r).get())));
					case LONG:
						return builder.value(op(context, left, ((JSONValue) r).doubleValue()));
					case BOOLEAN:
						return builder.value(op(context, left, ((JSONValue) r).booleanValue() ? (double) 1.0f : 0.0f));
					case STRING:
						return builder.value(op(context, left.toString(), ((JSONValue) r).stringValue()));
					case NULL:
						// return new JSONValue(null);
					case ARRAY:
					case FRAME:
					case OBJECT:
						return op(context, left, r);
				}
				break;
			}
			case STRING: {
				String left = ((JSONValue) l).stringValue();
				switch (r.getType()) {
					case DOUBLE:
					case LONG:
					case STRING:

						return builder.value(op(context, left, ((JSONValue) r).stringValue()));
					case NULL:
					case ARRAY:
					case FRAME:
					case BOOLEAN:
					case OBJECT:
						return op(context, left, r);
				}
				break;
			}
			case NULL:
				return builder.value();
		}
		throw new ExecutionException("not implemented yet");
	}

	protected JSON op(AsyncExecutionContext<JSON> context, JSONArray l, JSONArray r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> context, JSONObject l, JSONObject r) {
		return builder.value();
	}

	protected Long op(AsyncExecutionContext<JSON> context, Long l, Long r) {
		return null;
	}

	protected Boolean op(AsyncExecutionContext<JSON> context, Boolean l, Boolean r) {
		return null;
	}

	protected Double op(AsyncExecutionContext<JSON> context, Double l, Double r) {
		return null;
	}

	protected String op(AsyncExecutionContext<JSON> context, String l, String r) {
		return null;
	}

	protected JSON op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONArray r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONObject r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> eng, JSONObject l, JSON r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> context, Boolean l, JSON r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> context, Long l, JSON r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> context, Double l, JSON r) {
		return builder.value();
	}

	protected JSON op(AsyncExecutionContext<JSON> context, String l, JSON r) {
		return builder.value();
	}
}
