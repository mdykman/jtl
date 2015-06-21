package org.dykman.jtl.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.jsonLexer;
import org.dykman.jtl.jsonParser;
import org.dykman.jtl.jsonParser.JsonContext;
import org.dykman.jtl.core.engine.CollectionFactory;
import org.dykman.jtl.core.engine.MapFactory;
import org.dykman.jtl.core.parser.DataValue;
import org.dykman.jtl.core.parser.DataVisitor;

public class JSONBuilderImpl implements JSONBuilder {

	final MapFactory<String, JSON> mf;
	final CollectionFactory<JSON> cf;

	
	public JSONBuilderImpl(MapFactory<String,JSON> mf,CollectionFactory<JSON> cf ) {
		this.mf = mf;
		this.cf = cf;
	}

	public JSONBuilderImpl() {
		mf = new MapFactory<String, JSON>() {
			@Override public Map<String, JSON> createMap() { return new LinkedHashMap<>(); }
			@Override public Map<String, JSON> createMap(int cap) {	return new LinkedHashMap<>(cap); } 
			@Override public Map<String, JSON> copyMap(Map<String, JSON> rhs) {	return new LinkedHashMap<>(rhs); } };
		cf = new CollectionFactory<JSON>() { 
			@Override public Collection<JSON> createCollection() {return new ArrayList<>(); }
			@Override public Collection<JSON> createCollection(int cap) { return new ArrayList<>(cap); } 
			@Override public Collection<JSON> copyCollection(Collection<JSON> rhs) { return new ArrayList<>(rhs); } };
	}
	private JSON sign(JSON j) {
		j.setBuilder(this);
		return j;
	}

	@Override
	public JSONValue value() {
		JSONValue v = new JSONValue(null);
		return (JSONValue) sign(v);
	}

	@Override
	public JSONValue value(Number number) {
		JSONValue v = new JSONValue(null,number);
		return (JSONValue) sign(v);
	}

	@Override
	public JSONValue value(String string) {
		JSONValue v = new JSONValue(null,string);
		return (JSONValue) sign(v);
	}

	@Override
	public JSONValue value(Boolean b) {
		JSONValue v = new JSONValue(null,b);
		return (JSONValue) sign(v);
	}

	@Override
	public JSONObject object(JSON parent) {
		JSONObject r = new JSONObject(parent,mf.createMap());
		return (JSONObject) sign(r);
	}
	@Override
	public JSONObject object(JSON parent,int cap) {
		JSONObject r = new JSONObject(parent,mf.createMap(cap));
		return (JSONObject) sign(r);
	}

	@Override
	public JSONArray array(JSON parent,boolean bound) {
		JSONArray r = new JSONArray(parent,cf.createCollection(),bound);
		return (JSONArray) sign(r);
	}
	@Override
	public Frame frame() {
		Frame f = new Frame(cf.createCollection());
		f.setBuilder(this);
		return f;
	}
	@Override
	public Frame frame(Frame f) {
		Collection<JSON> cc = cf.copyCollection(f.collection());
		return new Frame(cc);
	}

	@Override
	public JSONArray array(JSON parent) {
		JSONArray r = new JSONArray(parent,cf.createCollection());
		return (JSONArray) sign(r);
	}
	@Override
	public JSONArray array(JSON parent,int cap) {
		JSONArray r = new JSONArray(parent,cf.createCollection(cap));
		return (JSONArray) sign(r);
	}

	@Override
	public JSON parse(InputStream in) 
			throws IOException {
			return parse( new jsonLexer(new ANTLRInputStream(in)));
		}
	@Override
	public JSON parse(File in) 
			throws IOException {
			return parse( new FileInputStream(in));
		}
	
	@Override
	public JSON parse(String in) 
			throws IOException {
			return parse( new jsonLexer(new ANTLRInputStream(in)));
		}
	protected JSON parse(jsonLexer lexer) 
			throws IOException {
			jsonParser parser = new jsonParser(new CommonTokenStream(lexer));
			//parser.setTrace(true);
			JsonContext tree = parser.json();
			DataVisitor visitor = new DataVisitor(this);
			DataValue<JSON> v = visitor.visitJson(tree);
			if(v!=null && v.value != null) v.value.setBuilder(this);
			if(v == null) return  null;
			v.value.setBuilder(this);
			v.value.lock();
			return v.value;
			
		}

	
}