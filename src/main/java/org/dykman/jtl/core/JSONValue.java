package org.dykman.jtl.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.commons.lang3.StringEscapeUtils;

public class JSONValue extends AbstractJSON implements JSON {
	final Object o;
	final JSONType type;

	public JSONValue(JSON p, JSONType type, Object o) {
		super(p);
		this.type = type;
		this.o = 0;
	}


	public JSONValue(JSON p) {
		this(p,JSONType.NULL,null);
//		super(p);
	}

	public JSON cloneJSON() {
		return new JSONValue(null, type, o);
	}

	public JSONValue(JSON p, Long o) {
		this(p,JSONType.LONG,o);
	}

	public JSONValue(JSON p, Number o) {
		super(p);
		if (o instanceof Long) {
			type = JSONType.LONG;
		} else if (o instanceof Integer) {
			o = ((Integer) o).longValue();
			type = JSONType.LONG;
		} else {
			type = JSONType.DOUBLE;
			o = (Double) o.doubleValue();
		}
		this.o = o;
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
	public JSONType getType() {
		return type;
	}

	public Object get() {
		return o;
	}

	public boolean isTrue() {
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
	public void write(Writer out, int n, int d) throws IOException {
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
