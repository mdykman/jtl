package org.dykman.jtl.json;

import java.io.IOException;
import java.io.Writer;

public interface JSON extends Comparable<JSON> {
	public String getName();
	public void setName(String s);
	public Integer getIndex() ;
	public void setIndex(Integer index);

	public JSON getParent();
	public JSON setParent(JSON p);
	public void lock();
	public JSONType getType();
	public boolean isValue();
	public boolean isTrue();
	public boolean isNumber();
	
	public JSON cloneJSON();
	public boolean equals(JSON r);
	public int compareTo(JSON r);
	public String path();
	public void path(StringBuilder sb);
	public void write(Writer out,int indent,boolean fq)
		throws IOException;
	public String toString(boolean fq);
   public String toString(int n, boolean fq);
	
	void write(Writer out,int indent, int depth,boolean fq)
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
		FRAME,
	};

}
