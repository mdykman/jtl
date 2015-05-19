package org.dykman.jtl.core;

import java.io.IOException;
import java.io.Writer;

public interface JSON {
	public String getName();
	public void setName(String s);
	public Integer getIndex() ;
	public void setIndex(Integer index);

	public JSON getParent();
	public JSON setParent(JSON p);
	public void lock();
	public JSONType getType();
	public boolean isFalse();
	
	public void write(Writer out,int indent)
		throws IOException;
	void write(Writer out,int indent, int depth)
			throws IOException;

	public enum JSONType {
		BOOLEAN,
		LONG,
		DOUBLE,
		STRING,
		OBJECT,
		ARRAY,
		NULL,
	};

}
