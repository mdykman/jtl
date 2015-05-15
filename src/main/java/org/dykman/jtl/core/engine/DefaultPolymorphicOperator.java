package org.dykman.jtl.core.engine;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;

public class DefaultPolymorphicOperator extends PolymorphicOperator {

	@Override
	protected JSON op(AsyncEngine<JSON> eng, JSONArray l, JSONArray r) {
		return new JSONValue(null);
	}

	@Override
	protected JSON op(AsyncEngine<JSON> eng, JSONObject l, JSONObject r) {
		return new JSONValue(null);
	}

	@Override
	protected Boolean op(AsyncEngine<JSON> eng, Boolean l, Boolean r) {
		return null;
	}

	@Override
	protected Long op(AsyncEngine<JSON> eng, Long l, Long r) {
		return null;
	}

	@Override
	protected Double op(AsyncEngine<JSON> eng, Double l, Double r) {
		return null;
	}

	@Override
	protected String op(AsyncEngine<JSON> eng, String l, String r) {
		return null;
	}

}
