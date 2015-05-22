package org.dykman.jtl.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.commons.lang3.StringEscapeUtils;


public class JSONValue extends AbstractJSON implements JSON {
	final Object o;
	final JSONType type;

	public JSONValue(JSON p) {
		super(p);
		type = JSONType.NULL;
		this.o = null;

	}

	public JSONValue(JSON p, Long o) {
		super(p);
		type = JSONType.LONG;
		this.o = o;
	}
	public JSONValue(JSON p, Number o) {
		super(p);
		if(o instanceof Long) {
			type = JSONType.LONG;
		} else if(o instanceof Integer) {
			o = ((Integer)o).longValue();
			type = JSONType.LONG;
		} else {
			type = JSONType.DOUBLE;
			o = (Double) o.doubleValue();
		}
		this.o = o;
	}
	public JSONValue(JSON p, Double o) {
		super(p);
		type = JSONType.DOUBLE;
		this.o = o;
	}
	public JSONValue(JSON p, Boolean o) {
		super(p);
		type = JSONType.BOOLEAN;
		this.o = o;
	}
	public JSONValue(JSON p, String o) {
		super(p);
		type = JSONType.STRING;
		this.o = o;
	}
	@Override
	public JSONType getType() {
		return type;
	}

	public Object get() {
		return o;
	}
	
	public boolean isFalse() {
		return o == null;
	}
	public Boolean booleanValue() {
		if(o instanceof Boolean) {
			return (Boolean) o;
		}
		if(o instanceof Number) {
			return ((Number) o).floatValue() != 0;
		}
		return null;
	}
	public String stringValue() {
		return o == null ? null : o.toString();
	}
	
	public Double doubleValue() {
		if(o instanceof Double) {
			return (Double) o;
		} else if (o instanceof Long) {
			return ((Long)o).doubleValue();
		}
		try {
			return Double.parseDouble(o.toString());
		} catch(NumberFormatException e) {}
		return null;
	}
	public Long longValue() {
		if(o instanceof Double) {
			return ((Double) o).longValue();
		} else if (o instanceof Long) {
			return (Long)o;
		}
		try {
			return Long.parseLong(o.toString());
		} catch(NumberFormatException e) {}
		return null;
	}


	@Override
	public void write(Writer out,int n,int d) 
		throws IOException {
		if(o==null) {
			out.write("null");
		} else {
			if(o instanceof Boolean) {
				out.write(((Boolean)o) ? "true" : "false");
			} else if(o instanceof String) {
				out.write('"');
				out.write(StringEscapeUtils.escapeJson(o.toString()));
				out.write('"');
			} else {
				out.write(o.toString());
			}
		}
	}

}
