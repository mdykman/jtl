package org.dykman.jtl.core.parser;

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
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.engine.CollectionFactory;
import org.dykman.jtl.core.engine.MapFactory;

public class JSONBuilder {

	MapFactory<String, JSON> mf;
	CollectionFactory<JSON> cf;
	
	public JSONBuilder(MapFactory<String,JSON> mf,CollectionFactory<JSON> cf ) {
		this.mf = mf;
		this.cf = cf;
	}

	public JSONBuilder() {
		mf = new MapFactory<String, JSON>() {
			@Override public Map<String, JSON> createMap() { return new LinkedHashMap<>(); }
			@Override public Map<String, JSON> createMap(int cap) {	return new LinkedHashMap<>(cap); } };
		cf = new CollectionFactory<JSON>() { 
			@Override public Collection<JSON> createCollection() {return new ArrayList<>(); }
			@Override public Collection<JSON> createCollection(int cap) { return new ArrayList<>(cap); } };
	}
	
	public Map<String,JSON> map() {
		return mf.createMap();
	}
	public Map<String,JSON> map(int c) {
		return mf.createMap(c);
	}
	public Collection<JSON> collection() {
		return cf.createCollection();
	}
	public Collection<JSON> collection(JSONArray arr) {
		Collection<JSON> cc = cf.createCollection();
		cc.addAll(arr.collection());
		return cc;
	}
	public Collection<JSON> collection(int cap) {
		return cf.createCollection(cap);
	}
	
	public JSONObject object(JSON parent,Map<String, JSON> map) {

		JSONObject r = new JSONObject(parent,map);
		r.setBuilder(this);
		return r;
	}
	public JSONObject object(JSON parent) {
		JSONObject r = new JSONObject(parent,mf.createMap());
		r.setBuilder(this);
		return r;
	}
	
	public JSONArray array(JSON parent) {
		JSONArray r = new JSONArray(parent,cf.createCollection());
		r.setBuilder(this);
		return r;
	}
	public JSONArray array(JSON parent,Collection<JSON> col) {
		JSONArray r = new JSONArray(parent,col);
		r.setBuilder(this);
		return r;
	}
	
	public JSON parse(InputStream in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}
	
	public JSON parse(String in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}

	public JSON parse(jtlLexer lexer) 
			throws IOException {
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
			JtlContext tree = parser.jtl();
			DataVisitor visitor = new DataVisitor(this);
			DataValue<JSON> v = visitor.visit(tree);
			v.value.setBuilder(this);
			return v.value;
			
		}

	
}