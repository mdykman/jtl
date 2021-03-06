package org.dykman.jtl.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJSON implements JSON {
	JSON parent;
	String name;
	boolean locked = false;
	Integer index = null;
	JSONBuilder builder = null;
	static final int charbuffer = 4096;

	static ThreadLocal<Collator> collatort = new ThreadLocal<>();

	static Logger logger = LoggerFactory.getLogger(JSON.class);

	static final char[] SPACES = new char[charbuffer];
	static {
		Arrays.fill(SPACES, ' ');
	}

	public AbstractJSON(JSON parent) {
		this.parent = parent;
		if(collatort.get()==null) {
			collatort.set(Collator.getInstance(Locale.getDefault()));
		}
	}


	@Override
	public String stringValue() {
		return toString();
	}

	@Override
	public boolean isValue() {
		return false;
	}
	public boolean equals(Object o) {
		if(!(o instanceof JSON)) return false;
		return equals((JSON)o);
	}
	public int compareTo(JSON r) {
		JSONType rtype = r.getType();
		switch(getType()) {
		case OBJECT:
			if(rtype != JSONType.OBJECT) return 1;
			return ((JSONObject) this).deepCompare((JSONObject) r);
		case LIST:
		case ARRAY:
			if(rtype != JSONType.ARRAY && rtype != JSONType.LIST) return rtype == JSONType.OBJECT ? -1 : 1;
//			if(rtype != JSONType.OBJECT) return 1;
			return ((JSONArray) this).deepCompare((JSONArray) r);
		case BOOLEAN:
			if(rtype == JSONType.NULL) return 1;
			if(rtype != JSONType.BOOLEAN) return -1;
			boolean lb =((JSONValue)this).booleanValue();
			boolean rb =((JSONValue)r).booleanValue();
			return lb == rb ? 0 : lb  ? 1 : 0;
		case DOUBLE:
		case LONG:
			if(rtype == JSONType.NULL || rtype == JSONType.BOOLEAN) return -1;
			if(rtype == JSONType.LONG || rtype == JSONType.DOUBLE) {
				double rd = ((JSONValue)this).doubleValue() - (((JSONValue)r).doubleValue());
				return rd> 0 ? 1 : rd < 0 ? -1 : 0;
			} else {
				return 1;
			}
		case NULL:
			if(rtype == JSONType.NULL) return 0;
			return -1;
		case STRING:
			if(rtype == JSONType.ARRAY || rtype == JSONType.OBJECT) return -1;
			if(rtype !=getType()) return 1;
			return collatort.get().compare(((JSONValue)this).stringValue(), ((JSONValue)r).stringValue());
		}
		logger.error("failed to match type in compareTo(");
		// TODO:: should throw an exception here
		return -1;
	}
	public void raise(String msg) {
		throw new RuntimeException(msg);
	}

	protected void writeAsString(Writer writer, String s) throws IOException {
		writer.write('"');
//		System.err.println("writeAsString: in " + s);
//		System.err.println("writeAsString: out " + StringEscapeUtils.escapeJson(s));
//System.err.println("writeAsString: in " + s);
		s = StringEscapeUtils.escapeJson(s);
//System.err.println("writeAsString: out " + s);
		writer.write(s);
		writer.write('"');

	}

	protected void indent(Writer writer, int indent, int depth)
			throws IOException {
		int n = indent * depth;
		while (n > SPACES.length) {
			writer.write(SPACES, 0, SPACES.length);
			n -= SPACES.length;
		}
		if (n > 0)
			writer.write(SPACES, 0, n);
	}

	@Override
	public void path(StringBuilder sb) {
		if (parent != null)
			parent.path(sb);
		if (name != null) {
			sb.append("/");
			sb.append(name);
		} else if (index != null) {
			sb.append("[").append(index.toString()).append("]");
		} else {
			sb.append("/");
//			builder.append("(undefined)");
		}
	}

	@Override
	public boolean isNumber() {
		JSONType type = getType();
		return type == JSONType.LONG || type == JSONType.DOUBLE || type == JSONType.BOOLEAN;
	}
	@Override
	public String path() {
		StringBuilder builder = new StringBuilder();
		path(builder);
		return builder.toString();
	}
	
	@Override
	abstract public JSON cloneJSON(boolean deep);
	@Override
	public JSON cloneJSON() {
		return cloneJSON(true);
	}


	@Override
	public void lock() {
//		locked = true;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String s) {
//		if (locked)
//			raise("container is locked");
		name = s;
	}

	@Override
	public JSON getParent() {
		return parent;
	}

	@Override
	public JSON setParent(JSON p) {
		if (locked)
			raise("container is locked");
		 parent = p;
		 return this;
	}

	@Override
	public void write(Writer out, int indent,boolean fq) throws IOException {
		write(out, indent, 0,fq);
		if (indent > 0) {
			out.write("\n");
		}
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}
@Override
	public JSONBuilder builder() {
		return builder;
	}

	public void setBuilder(JSONBuilder builder) {
		this.builder = builder;
	}
	@Override
	public final String toString() {
		return toString(false);
	}

	  @Override
	   public final String toString(int n, boolean fq) {
	      StringWriter writer = new StringWriter();
	      try {
	         write(writer,n,fq);
	      } catch(IOException e) {
	         throw new RuntimeException("error while rendering");
	      }
	      return writer.toString();
	   }
	@Override
	public final String toString(boolean fq) {
		StringWriter writer = new StringWriter();
		try {
			write(writer,0,fq);
		} catch(IOException e) {
			throw new RuntimeException("error while rendering");
		}
		return writer.toString();
	}
}
