package org.dykman.jtl.core.engine;

import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.jtlLexer;
import org.dykman.jtl.jtlParser;
import org.dykman.jtl.jtlParser.JtlContext;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONBuilderImpl;
import org.dykman.jtl.core.parser.DataValue;
import org.dykman.jtl.core.parser.DataVisitor;


public abstract class JSONFactory {

	protected JSONFactory() {
		// TODO Auto-generated constructor stub
	}

	public static JSON parse(InputStream in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}
	public static JSON parse(String in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}

	public static JSON parse(jtlLexer lexer) 
			throws IOException {
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
			JtlContext tree = parser.jtl();
			DataVisitor visitor = new DataVisitor(new JSONBuilderImpl());
			DataValue<JSON> v = visitor.visit(tree);
			return v.value;
			
		}
}
