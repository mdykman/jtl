package org.dykman.jtl.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.jsonLexer;
import org.dykman.jtl.jsonParser;
import org.dykman.jtl.jsonParser.JsonContext;
import org.dykman.jtl.factory.CollectionFactory;
import org.dykman.jtl.factory.MapFactory;

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
	public JSON value(Object o) {
		if(o == null) return value();
		if(o instanceof Boolean) return value((Boolean)o);
		if(o instanceof BigInteger) {
			BigInteger maxLong = new BigInteger(Long.toString(Long.MAX_VALUE));
			if(((BigInteger)o).compareTo(maxLong)>0) {
				return value(((BigInteger)o).doubleValue());
			}
		}
		if(o instanceof Number) {
			if(o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof BigInteger) {
				return value(((Number)o).longValue());
			}
			return value(((Number)o).doubleValue());
			
		} else if(o instanceof byte[]) {
			JSONArray arr = array(null);
			for(Byte b : (byte[])o) {
				int ii  = b.intValue();
				arr.add(value((256 + ii) % 256));
			}
			arr.lock();
			return arr;
		}else {
			return value(o.toString());
		}
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
		return (Frame) sign(f);
	}
	
//	@Override
	public Frame frame(Frame f) {
		Collection<JSON> cc = cf.copyCollection(f.collection());
		return (Frame) sign(new Frame(cc));
	}
	@Override
	public Frame frame(JSON parent) {
//		Collection<JSON> cc = cf.copyCollection(f.collection());
		return (Frame) sign(new Frame(parent,cf.createCollection()));
	}

	@Override
	public JSONArray array(JSON parent) {
		JSONArray r = new JSONArray(parent,cf.createCollection());
		return (JSONArray) sign(r);
	}
	@Override
	public JSONArray array(JSON parent,Collection<JSON> cc) {
		JSONArray r = new JSONArray(parent,cc);
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
	public JSON parse(Reader in) 
			throws IOException {
		return parse(new jsonLexer(new ANTLRInputStream(in)));
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
			v.value.lock();
			return v.value;
		}

	
}