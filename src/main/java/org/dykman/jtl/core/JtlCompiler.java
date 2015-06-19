package org.dykman.jtl.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.jtlLexer;
import org.dykman.jtl.jtlParser;
import org.dykman.jtl.jtlParser.JtlContext;
import org.dykman.jtl.core.engine.future.InstructionFuture;
import org.dykman.jtl.core.engine.future.InstructionFutureValue;
import org.dykman.jtl.core.engine.future.InstructionFutureVisitor;

public class JtlCompiler {
	final JSONBuilder jsonBuilder;
	boolean trace;
	boolean profile;

	public JtlCompiler(JSONBuilder jsonBuilder) {
		this(jsonBuilder,false,false);
	}
	public JtlCompiler(JSONBuilder jsonBuilder,boolean trace, boolean profile) {
		this.jsonBuilder = jsonBuilder;
		this.trace= trace;
		this.profile = profile;
	}
	
	public InstructionFuture<JSON> parse(InputStream in) 
		throws IOException {
		return parse(in, trace, profile);
	}
	
	public InstructionFuture<JSON> parse(InputStream in,boolean trace,boolean profile) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)),trace,profile);
		}
	
	public InstructionFuture<JSON> parse(File in,boolean trace,boolean profile) 
			throws IOException {
		return parse(new FileInputStream(in),trace,profile);
		}
	public InstructionFuture<JSON> parse(File in) 
		throws IOException {
		return parse(in, trace, profile);
	}

	public InstructionFuture<JSON> parse(String in) 
		throws IOException {
		return parse(in, trace, profile);
	}
	
	public InstructionFuture<JSON> parse(String in,boolean trace,boolean profile) 
			throws IOException {
			return parse( new jtlLexer(new ANTLRInputStream(in)),trace,profile);
		}
	protected InstructionFuture<JSON> parse(jtlLexer lexer) 
		throws IOException {
		return parse(lexer, trace, profile);
	}
	
	protected InstructionFuture<JSON> parse(jtlLexer lexer,boolean trace,boolean profile) 
			throws IOException {
	//	lexer.
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
			parser.setTrace(trace);
			parser.setProfile(profile);
//			parser.getCurrentToken();
			JtlContext tree = parser.jtl();
			InstructionFutureVisitor visitor = new InstructionFutureVisitor(jsonBuilder);
			InstructionFutureValue<JSON> v = visitor.visit(tree);
			return v.inst;
		}

}
