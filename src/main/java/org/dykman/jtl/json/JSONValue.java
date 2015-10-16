package org.dykman.jtl.json;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.StringEscapeUtils;

public class JSONValue extends AbstractJSON implements JSON {
	final Object o;
	final JSONType type;

	public JSONValue(JSON p, Number io) {
		super(p);
		if(io == null) {
			this.o = io;
			type = JSONType.NULL;
		} else if (io instanceof Long) {
			this.o = io;
			type = JSONType.LONG;
		} else if (io instanceof Integer) {
			this.o = io.longValue();
			type = JSONType.LONG;
		} else {
			this.o = io.doubleValue();
			type = JSONType.DOUBLE;
		}
	}

	@Override
	public boolean isValue() {
		return true;
	}
	protected JSONValue(JSON p, JSONType type, Object o) {
		super(p);
		if(o == null) this.type = JSONType.NULL;
		else this.type = type;
		this.o = o;
	}

	public JSONValue(JSON p) {
		this(p,JSONType.NULL,null);
	}

	public JSONValue(JSON p, Long o) {
		this(p,JSONType.LONG,o);
	}

	public JSONValue(JSON p, Double o) {
		this(p,JSONType.DOUBLE,o);
	}

	public JSONValue(JSON p, Boolean o) {
		this(p,JSONType.BOOLEAN,o);
	}

	public JSONValue(JSON p, String o) {
		this(p,JSONType.STRING,o);
	}

	@Override
	public JSON cloneJSON() {
		return new JSONValue(parent, type, o);
	}


	@Override
	public JSONType getType() {
		return type;
	}

	public Object get() {
		return o;
	}

	public boolean isTrue() {
		if(type == JSONType.STRING) {
			return o != null && ((String) o).length() > 0;
		}
		return booleanValue();
	}

	public Boolean booleanValue() {
		switch (type) {
		case NULL:
			return false;
		case BOOLEAN:
			return (Boolean) o;
		case DOUBLE:
			return ((Double) o) != 0;
		case LONG:
			return ((Long) o) != 0;
		case STRING:
				return "true".equalsIgnoreCase(o.toString());
		default:
			return false;
		}
	}

	@Override
	public int hashCode() {
		if(o == null || getType() == JSONType.NULL) return 0;	
		String s = stringValue();
		return s.hashCode();
	}
	public String stringValue() {
		if (type == JSONType.BOOLEAN)
			return (Boolean) o ? "true" : "false";
		return o == null ? null : o.toString();
	}

	public Double doubleValue() {
		switch (type) {
		case NULL:
			return null;
		case BOOLEAN:
			return (Boolean) o ? (double) 1f : 0f;
		case DOUBLE:
			return (Double) o;
		case LONG:
			return ((Long) o).doubleValue();
		case STRING:
			try {
				return Double.parseDouble(o.toString());
			} catch (NumberFormatException e) {}
		default:
			return null;
		}
	}

	public Long longValue() {
		switch (type) {
		case NULL:
			return null;
		case BOOLEAN:
			return (Boolean) o ? 1L : 0L;
		case DOUBLE:
			return  ((Double) o).longValue();
		case LONG:
			return (Long) o;
		case STRING:
			try {
				return Long.parseLong(o.toString());
			} catch (NumberFormatException e) {}
		default:
			return null;
		}
		}


	public boolean equals(JSON r) {
		return 0 == compareTo(r);
	}
	public boolean __equals(JSON r) {
		if (r == null)
			return false;
		if (type != r.getType())
			return false;
		JSONValue rv = (JSONValue) r;
		switch (type) {
		case NULL:
			return true;
		case BOOLEAN:
			return (Boolean) o == rv.booleanValue();
		case LONG:
			return (Long) o == rv.longValue();
		case DOUBLE:
			return (Double) o == rv.doubleValue();
		case STRING:
			return ((String) o).equals(rv.stringValue());
		default: return false;
		}
	}

	@Override
	public void write(Writer out, int n, int d,boolean fq) throws IOException {
		if (o == null) {
			out.write("null");
		} else {
			if (o instanceof Boolean) {
				out.write(((Boolean) o) ? "true" : "false");
			} else if (o instanceof String) {
				out.write('"');
				out.write(StringEscapeUtils.escapeJson(o.toString()));
				out.write('"');
			} else {
				out.write(o.toString());
			}
		}
	}
}
