package org.dykman.jtl.core;


public class JSONValue extends AbstractJSON {
	final Object o;
	final JSONType type;

	public JSONValue(JSON p) {
		super(p);
		type = JSONType.NULL;
		this.o = null;

	}

	@Override
	public JSONType getType() {
		return type;
	}

	public JSONValue(JSON p, Long o) {
		super(p);
		type = JSONType.LONG;
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
	public Object get() {
		return o;
	}
	
	public boolean isNull() {
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
}
