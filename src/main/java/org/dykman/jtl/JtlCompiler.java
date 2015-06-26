package org.dykman.jtl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.jtlLexer;
import org.dykman.jtl.jtlParser;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.jtlParser.JtlContext;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.future.InstructionFutureValue;
import org.dykman.jtl.future.InstructionFutureVisitor;
import org.dykman.jtl.future.SimpleExecutionContext;

import com.google.common.util.concurrent.ListeningExecutorService;

public class JtlCompiler {
	final JSONBuilder jsonBuilder;
	boolean trace;
	boolean profile;
	boolean imported;
/*
	public JtlCompiler(JSONBuilder jsonBuilder) {
		this(jsonBuilder,false,false,false);
	}
	
	*/
	public JtlCompiler(JSONBuilder jsonBuilder,boolean trace, boolean profile,boolean imported) {
		this.jsonBuilder = jsonBuilder;
		this.trace= trace;
		this.profile = profile;
		this.imported = imported;
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
			InstructionFutureVisitor visitor = new InstructionFutureVisitor(jsonBuilder,imported);
			InstructionFutureValue<JSON> v = visitor.visit(tree);
			return v.inst;
		}

	public static AsyncExecutionContext<JSON> createInitialContext(
			JSON config,
			InstructionFutureFactory factory,
			ListeningExecutorService les ) {

		SimpleExecutionContext context = new SimpleExecutionContext(factory.builder(),config);
		
		if(config.getType() == JSONType.OBJECT) {
			JSONObject conf = (JSONObject) config;
			JSONObject modules= (JSONObject)conf.get("modules");
			context.define("module", factory.loadModule(modules));
		}
		context.define("import", factory.importInstruction(config));
		context.define("error", factory.defaultError());
		context.define("group", factory.groupBy());
		context.define("file", factory.file());
		context.define("map", factory.map());
		context.define("unique", factory.unique());
		context.define("count", factory.count());
		context.define("sort", factory.sort(false));
		context.define("rsort", factory.sort(true));
		context.define("filter", factory.filter());
		context.define("params", factory.params());
		context.define("collate", factory.collate());
		
	
		
		context.setExecutionService(les);
		return context;
	}

}
