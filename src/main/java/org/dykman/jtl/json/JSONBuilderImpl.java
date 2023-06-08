package org.dykman.jtl.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.dykman.jtl.jsonLexer;
import org.dykman.jtl.jsonParser;
import org.dykman.jtl.jsonParser.JsonContext;
import org.dykman.jtl.factory.CollectionFactory;
import org.dykman.jtl.factory.MapFactory;

public class JSONBuilderImpl implements JSONBuilder {

	final MapFactory<String, JSON> mf;
	final CollectionFactory<JSON> cf;

	static ThreadLocal<Collator> collatort = new ThreadLocal<>();
	java.text.Collator collator = Collator.getInstance(Locale.getDefault());

	public JSONBuilderImpl(MapFactory<String, JSON> mf, CollectionFactory<JSON> cf) {
		this.mf = mf;
		this.cf = cf;
		
	}
	public static JSON NULL = new JSONValue(null);
	public static JSON ZERO = new JSONValue(null,0L);
	public static JSON ONE = new JSONValue(null,1L);

	
	public static JSON TRUE = new JSONValue(null,true);
	public static JSON FALSE = new JSONValue(null,false);

	private static Map<String, JSON> mapBuilder(Locale locale) {
		return new TreeMap<>(localeComparator(locale));
	}
	private static Map<String, JSON> mapBuilder(boolean canonical) {
		return canonical ? new TreeMap<>(localeComparator(Locale.getDefault())) : new LinkedHashMap<>();
	}

	private static Map<String, JSON> mapBuilder(boolean canonical, int cap) {
		return canonical ? new TreeMap<>(localeComparator(Locale.getDefault())) : new LinkedHashMap<>(cap);
	}

	private static Map<String, JSON> mapBuilder(boolean canonical, Map<String, JSON> rhs) {
		if (canonical) {
			TreeMap<String, JSON> m = new TreeMap<>(localeComparator(Locale.getDefault()));
			m.putAll(rhs);
			return m;
		} else {
			return new LinkedHashMap<>(rhs);
		}

	}
	public Map<String,JSON> map() {
		return mf.createMap();
	}
	public Map<String,JSON> map(Map<String,JSON> m) {
		return mf.copyMap(m);
	}
	
	public Collection<JSON> collection() {
		return cf.createCollection();
	}
	public Collection<JSON> collection(Collection<JSON> c) {
		return cf.copyCollection(c);
	}

	static Comparator<String> localeComparator(Locale locale) {
		 return new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Collator cc = collatort.get();
				if(cc == null) {
					cc = Collator.getInstance(locale);
						collatort.set(cc);
				}
				return cc.compare(o1, o2);
			}
		 };	
	}

	public JSONBuilderImpl() {
		this(false);
	}

	public JSONBuilderImpl(final boolean c) {
		this(new MapFactory<String, JSON>() {
			@Override
			public Map<String, JSON> createMap() {
				return mapBuilder(c);
			}

			@Override
			public Map<String, JSON> createMap(int cap) {
				return mapBuilder(c, cap);
			}

			@Override
			public Map<String, JSON> copyMap(Map<String, JSON> rhs) {
				return mapBuilder(c, rhs);
			}

			@Override
			public Map<String, JSON> createMap(Locale locale) {
				return mapBuilder(locale);
			}
		}, new CollectionFactory<JSON>() {
			@Override
			public Collection<JSON> createCollection() {
				return new ArrayList<>();
			}

			@Override
			public Collection<JSON> createCollection(int cap) {
				return new ArrayList<>(cap);
			}

			@Override
			public Collection<JSON> copyCollection(Collection<JSON> rhs) {
				return new ArrayList<>(rhs);
			}
		});
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
		if (o == null)
			return NULL;
		if (o instanceof JSON) {
			return (JSON) o;
		}
		if (o instanceof Boolean)
			return value((Boolean) o);
		if (o instanceof BigInteger) {
			BigInteger maxLong = new BigInteger(Long.toString(Long.MAX_VALUE));
			if (((BigInteger) o).compareTo(maxLong) > 0) {
				return value(((BigInteger) o).doubleValue());
			}
		}
		if (o instanceof Number) {
			if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof BigInteger) {
				return value(((Number) o).longValue());
			}
			return value(((Number) o).doubleValue());

		} else if (o instanceof byte[]) {
			return value(new String((byte[])o));
		} else {
			return value(o.toString());
		}
	}

	@Override
	public JSONValue value(Number number) {
		JSONValue v = new JSONValue(null, number);
		
		return (JSONValue) sign(v);
	}

	@Override
	public JSONValue value(String string) {
		JSONValue v = new JSONValue(null, string);
		return (JSONValue) sign(v);
	}

	@Override
	public JSONValue value(Boolean b) {
		JSONValue v = new JSONValue(null, b);
		return (JSONValue) sign(v);
	}

	@Override
	public JSONObject object(JSON parent) {
		JSONObject r = new JSONObject(parent, mf.createMap());
		return (JSONObject) sign(r);
	}

	@Override
	public JSONObject object(JSON parent, Map<String,JSON> map) {
		JSONObject r = new JSONObject(parent, map);
		return (JSONObject) sign(r);
	}

	@Override
	public JSONObject object(JSON parent, int cap) {
		JSONObject r = new JSONObject(parent, mf.createMap(cap));
		return (JSONObject) sign(r);
	}


	@Override
	public JSONObject object(JSON parent,Locale locale) {
		JSONObject r = new JSONObject(null,mf.createMap(locale));
		return (JSONObject) sign(r);
	}

	
	@Override
	public JList list() {
		JList f = new JList(cf.createCollection());
		return (JList) sign(f);
	}

	@Override
	public JList list(JSON parent, Collection<JSON> col) {
		return (JList) sign(new JList(parent, col));
	}

	// @Override
	public JList frame(JList f) {
		Collection<JSON> cc = cf.copyCollection(f.collection());
		return (JList) sign(new JList(cc));
	}

	@Override
	public JList list(JSON parent) {
		// Collection<JSON> cc = cf.copyCollection(f.collection());
		return (JList) sign(new JList(parent, cf.createCollection()));
	}

	@Override
	public JSONArray array(JSON parent, boolean bound) {
		JSONArray r = new JSONArray(parent, cf.createCollection(), bound);
		return (JSONArray) sign(r);
	}

	@Override
	public JSONArray array(JSON parent) {
		JSONArray r = new JSONArray(parent, cf.createCollection());
		return (JSONArray) sign(r);
	}

	@Override
	public JSONArray array(JSON parent, Collection<JSON> cc) {
		JSONArray r = new JSONArray(parent, cc);
		return (JSONArray) sign(r);
	}

	@Override
	public JSONArray array(JSON parent, int cap) {
		JSONArray r = new JSONArray(parent, cf.createCollection(cap));
		return (JSONArray) sign(r);
	}

	@Override
	public JSON parse(InputStream in) throws IOException {
		// jsonLexer ll= new jsonLexer(new ANTLRInputStream(in));

		return parse(new jsonLexer(new ANTLRInputStream(in)));
	}

	@Override
	public JSON parse(File in) throws IOException {
		return parse(new FileInputStream(in));
	}

	@Override
	public JSON parse(Reader in) throws IOException {
		return parse(new jsonLexer(new ANTLRInputStream(in)));
	}

	@Override
	public JSON parse(String in) throws IOException {
		return parse(new jsonLexer(new ANTLRInputStream(in)));
	}

	public jsonParser createParser(String in) {
		return createParser(new jsonLexer(new ANTLRInputStream(in)));
	}

	public jsonParser createParser(InputStream in) throws IOException {
		return createParser(new jsonLexer(new ANTLRInputStream(in)));
	}

	public jsonParser createParser(Reader in) throws IOException {
		return createParser(new jsonLexer(new ANTLRInputStream(in)));
	}

	public jsonParser createParser(File in) throws IOException {
		// new FileInputStream(in)
		return createParser(new FileInputStream(in));
	}

	public jsonParser createParser(jsonLexer lexer) {
		return new jsonParser(new CommonTokenStream(lexer));
	}

	public JSON parse(jsonLexer lexer) throws IOException {
		jsonParser parser = new jsonParser(new CommonTokenStream(lexer));
		JsonContext tree = parser.json();
		DataVisitor visitor = new DataVisitor(this);
		DataValue<JSON> v = visitor.visitJson(tree);
		if (v != null && v.value != null)
			v.value.setBuilder(this);
		if (v == null)
			return null;
		v.value.lock();
		return v.value;
	}

	public JSON parseSequence(jsonParser parser) throws IOException {
		parser.getCurrentToken();

		if (Token.EOF == parser.getCurrentToken().getType()) {
			return null;
		}
		JsonContext tree = parser.json();
		// System.out.println("one");
		if (tree == null)
			return null;
		// System.out.println("two");

		// if(tree.value())
		if (tree.value().getChild(0) == null) {
			// System.out.println("three");
			return null;
		}
		// System.out.println("four");
		DataVisitor visitor = new DataVisitor(this);
		// System.out.println("five");
		DataValue<JSON> v = visitor.visitJson(tree);
		if (v != null && v.value != null)
			v.value.setBuilder(this);
		if (v == null)
			return null;
		v.value.lock();
		return v.value;
	}

}
