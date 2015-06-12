package org.dykman.jtl.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import main.antlr.jtlLexer;

public interface JSONBuilder {
	public JSONValue value();
	public JSONValue value(Number number);
	public JSONValue value(String string);
	public JSONValue value(Boolean b);

//	public JSONObject object(JSON parent, Map<String, JSON> map);
	public JSONObject object(JSON parent);
	public JSONObject object(JSON parent, int cap);

	public JSONArray array(JSON parent,boolean bound);

	public JSONArray array(JSON parent);
	public JSONArray array(JSON parent, int cap);
//	public JSONArray array(JSON parent, Collection<JSON> col);

	public JSON parse(File in) throws IOException;
	public JSON parse(InputStream in) throws IOException;
	public JSON parse(String in) throws IOException;

}