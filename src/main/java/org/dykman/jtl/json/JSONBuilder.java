package org.dykman.jtl.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;

public interface JSONBuilder {
	public JSONValue value();
	public JSON value(Object o);
	public JSONValue value(Number number);
	public JSONValue value(String string);
	public JSONValue value(Boolean b);

//	public JSONObject object(JSON parent, Map<String, JSON> map);
	public JSONObject object(JSON parent);
	public JSONObject object(JSON parent, int cap);

	public JSONArray array(JSON parent,boolean bound);

	public JSONArray array(JSON parent);
	public JSONArray array(JSON parent, int cap);
	public JSONArray array(JSON parent, Collection<JSON> col);
	public Frame frame();
//	public Frame frame(Frame f);
	public Frame frame(JSON parent);

	public JSON parse(File in) throws IOException;
	public JSON parse(InputStream in) throws IOException;
	public JSON parse(Reader in) throws IOException;
	public JSON parse(String in) throws IOException;

}