package org.dykman.jtl;

import org.antlr.v4.runtime.Token;

public class JtlParseException extends RuntimeException {

	SourceInfo si;
	public JtlParseException(Token t) {
		SourceInfo si = SourceInfo.internal("parser");
		si.line = t.getLine();
		si.position = t.getCharPositionInLine();
		si.code = t.getText();
		this.si = si;
	}//289-682--2868
	
	public SourceInfo getSourceInfo() {
		return si;
	}
	
	public String report() {
		   StringBuilder builder = new StringBuilder();
		   builder.append("Exception after `").append(si.code)
		   		.append("': ").append(getLocalizedMessage())
		   		.append(" -- ").append(si.name).append(" ")
		   		.append(si.line).append(":").append(si.position);
		   return builder.toString();
		}

}

