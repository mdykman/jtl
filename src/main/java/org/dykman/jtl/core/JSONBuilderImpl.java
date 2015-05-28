package org.dykman.jtl.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import main.antlr.jtlLexer;
import main.antlr.jtlParser;
import main.antlr.jtlParser.JtlContext;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.core.engine.CollectionFactory;
import org.dykman.jtl.core.engine.MapFactory;
import org.dykman.jtl.core.parser.DataValue;
import org.dykman.jtl.core.parser.DataVisitor;

public class JSONBuilderImpl implements JSONBuilder {

	MapFactory<String, JSON> mf;
	CollectionFactory<JSON> cf;
	
	public JSONBuilderImpl(MapFactory<String,JSON> mf,CollectionFactory<JSON> cf ) {
		this.mf = mf;
		this.cf = cf;
	}

	public JSONBuilderImpl() {
		mf = new MapFactory<String, JSON>() {
			@Override public Map<String, JSON> createMap() { return new LinkedHashMap<>(); }
			@Override public Map<String, JSON> createMap(int cap) {	return new LinkedHashMap<>(cap); } };
		cf = new CollectionFactory<JSON>() { 
			@Override public Collection<JSON> createCollection() {return new ArrayList<>(); }
			@Override public Collection<JSON> createCollection(int cap) { return new ArrayList<>(cap); } };
	}

	
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#value()
	 */
	@Override
	public JSONValue value() {
		JSONValue v = new JSONValue(null);
		v.setBuilder(this);
		return v;
	}
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#value(java.lang.Number)
	 */
	@Override
	public JSONValue value(Number number) {
		JSONValue v = new JSONValue(null,number);
		v.setBuilder(this);
		return v;
	}
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#value(java.lang.String)
	 */
	@Override
	public JSONValue value(String string) {
		JSONValue v = new JSONValue(null,string);
		v.setBuilder(this);
		return v;
	}
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#value(java.lang.Boolean)
	 */
	@Override
	public JSONValue value(Boolean b) {
		JSONValue v = new JSONValue(null,b);
		v.setBuilder(this);
		return v;
	}

	

	@Override
	public JSONObject object(JSON parent) {
		JSONObject r = new JSONObject(parent,mf.createMap());
		r.setBuilder(this);
		return r;
	}
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#object(org.dykman.jtl.core.JSON, int)
	 */
	@Override
	public JSONObject object(JSON parent,int cap) {
		JSONObject r = new JSONObject(parent,mf.createMap(cap));
		r.setBuilder(this);
		return r;
	}
	
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#array(org.dykman.jtl.core.JSON)
	 */
	@Override
	public JSONArray array(JSON parent) {
		JSONArray r = new JSONArray(parent,cf.createCollection());
		r.setBuilder(this);
		return r;
	}
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#array(org.dykman.jtl.core.JSON, int)
	 */
	@Override
	public JSONArray array(JSON parent,int cap) {
		JSONArray r = new JSONArray(parent,cf.createCollection(cap));
		r.setBuilder(this);
		return r;
	}

	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#parse(java.io.InputStream)
	 */
	@Override
	public JSON parse(InputStream in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}
	
	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#parse(java.lang.String)
	 */
	@Override
	public JSON parse(String in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}

	/* (non-Javadoc)
	 * @see org.dykman.jtl.core.JSONBuilderI#parse(main.antlr.jtlLexer)
	 */
	
	protected JSON parse(jtlLexer lexer) 
			throws IOException {
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
			JtlContext tree = parser.jtl();
			DataVisitor visitor = new DataVisitor(this);
			DataValue<JSON> v = visitor.visit(tree);
			v.value.setBuilder(this);
			return v.value;
			
		}

	
}