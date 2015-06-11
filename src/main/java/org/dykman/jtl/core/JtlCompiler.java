package org.dykman.jtl.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import main.antlr.jtlLexer;
import main.antlr.jtlParser;
import main.antlr.jtlParser.JtlContext;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.core.engine.future.InstructionFuture;
import org.dykman.jtl.core.engine.future.InstructionFutureValue;
import org.dykman.jtl.core.engine.future.InstructionFutureVisitor;

public class JtlCompiler {
	final JSONBuilder jsonBuilder;

	public JtlCompiler(JSONBuilder jsonBuilder) {
		this.jsonBuilder = jsonBuilder;
	}
	public InstructionFuture<JSON> parse(InputStream in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}
	
	public InstructionFuture<JSON> parse(File in) 
			throws IOException {
		return parse(new FileInputStream(in));
		}
	
	public InstructionFuture<JSON> parse(String in) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)));
		}
	protected InstructionFuture<JSON> parse(jtlLexer lexer) 
			throws IOException {
	//	lexer.
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
//			parser.setTrace(true);
//			parser.setProfile(true);
//			parser.getCurrentToken();
			JtlContext tree = parser.jtl();
			InstructionFutureVisitor visitor = new InstructionFutureVisitor(jsonBuilder);
			InstructionFutureValue<JSON> v = visitor.visit(tree);
			return v.inst;
		}

}
