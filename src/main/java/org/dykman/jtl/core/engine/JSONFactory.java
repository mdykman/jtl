package org.dykman.jtl.core.engine;

import java.io.IOException;
import java.io.InputStream;

import main.antlr.jtlLexer;
import main.antlr.jtlParser;
import main.antlr.jtlParser.JtlContext;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.parser.DataValue;
import org.dykman.jtl.core.parser.DataVisitor;
import org.dykman.jtl.core.parser.JSONBuilder;

public abstract class JSONFactory {

	protected JSONFactory() {
		// TODO Auto-generated constructor stub
	}

	public static JSON parse(InputStream in) 
			throws IOException {
			jtlLexer lexer = new jtlLexer(new ANTLRInputStream(in));
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
			JtlContext tree = parser.jtl();
			DataVisitor visitor = new DataVisitor(new JSONBuilder());
			DataValue<JSON> v = visitor.visit(tree);
			return v.value;
			
		}
}
