package org.dykman.jtl.core;

import java.io.IOException;
import java.io.Writer;

import org.dykman.jtl.core.parser.JSONBuilder;

public interface JSON {
	public String getName();
	public void setName(String s);
	public Integer getIndex() ;
	public void setIndex(Integer index);

	public JSON getParent();
	public JSON setParent(JSON p);
	public void lock();
	public JSONType getType();
	public boolean isTrue();

	public boolean isNumber();
	public JSON cloneJSON();
	public boolean equals(JSON r);
	public int compare(JSON r);
	public String path();
	public void path(StringBuilder sb);
	public void write(Writer out,int indent)
		throws IOException;
	void write(Writer out,int indent, int depth)
			throws IOException;
	
	public void setBuilder(JSONBuilder builder);
	public JSONBuilder builder();

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
