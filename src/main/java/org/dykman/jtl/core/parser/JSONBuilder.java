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
	public static JSON parse(String str) {
		char[]cc = str.toCharArray();
		CharStream cs = new ANTLRInputStream(cc, cc.length);
		jtlLexer lexer = new jtlLexer(cs);
		return parse(lexer);
	}

	static JSON parse(jtlLexer lexer) {
		jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
		JtlContext tree = parser.jtl();
		DataVisitor visitor = new DataVisitor(new JSONBuilder());
		return visitor.visit(tree).value;		
	}
	public static JSON parse(InputStream in) 
			throws IOException {
			jtlLexer lexer = new jtlLexer(new ANTLRInputStream(in));
			return parse(lexer);
			
		}

	public JSONBuilder() {
		mf = new MapFactory<String, JSON>() {

			@Override
			public Map<String, JSON> createMap() {
				return new LinkedHashMap<>();
			}
			@Override
			public Map<String, JSON> createMap(int cap) {
				return new LinkedHashMap<>(cap);
//				return new HashMap<>(cap);
				//return new TreeMap<>();
			}
		};
		cf = new CollectionFactory<JSON>() {

			@Override
			public Collection<JSON> createCollection() {
				return new ArrayList<>();
			}
			@Override
			public Collection<JSON> createCollection(int cap) {
				return new ArrayList<>(cap);
			}
		};
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
	public Collection<JSON> collection(int cap) {
		return cf.createCollection(cap);
	}
	
	public JSONObject object(JSON parent,Map<String, JSON> map) {

		return new JSONObject(parent,map);
	}
	public JSONObject object(JSON parent) {

		return new JSONObject(parent,mf.createMap());
	}
	public JSONArray array(JSON parent) {
		return new JSONArray(parent,cf.createCollection());
	}
	public JSONArray array(JSON parent,Collection<JSON> col) {
		return new JSONArray(parent,col);
	}
}