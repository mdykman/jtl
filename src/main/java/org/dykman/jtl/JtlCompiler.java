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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;

import static org.dykman.jtl.future.InstructionFutureFactory.*;

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
			JSON data,
			JSON config,
			File f,
			JSONBuilder builder,
			ListeningExecutorService les ) {

		SimpleExecutionContext context = new SimpleExecutionContext(builder,
		      Futures.immediateCheckedFuture(data),config,f);
		context.setExecutionService(les);
		
		// configurable: import, extend
		if(config.getType() == JSONType.OBJECT) {
			JSONObject conf = (JSONObject) config;
			JSONObject modules= (JSONObject)conf.get("modules");
			context.define("module", loadModule(modules));
			context.define("import", importInstruction(config));
		}
		
		// general
		context.define("error", defaultError());
		context.define("params", params());

		// external data
		context.define("file", file());
		context.define("url", url());

		
		// string-oriented
      context.define("split", split());
      context.define("join", join());
      context.define("substr", substr());
		
		// list-oriented
		context.define("unique", unique());
		context.define("count", count());
		context.define("sort", sort(false));
		context.define("rsort", sort(true));
		context.define("filter", filter());
		context.define("contains", contains());

		// object-oriented
		context.define("group", groupBy());
		context.define("map", map());
		context.define("collate", collate());
		context.define("omap", omap());
		context.define("amend", amend());
      context.define("keys", keys());

// ??		context.define("apply", apply());
	
		// boolean type test only
      context.define("null", isNull());
      context.define("value", isValue());
		context.define("object", isObject());

		// with 0 args, they return boolean type test
		// with 1 arg, attempts to coerce to the specified type
		context.define("array", isArray());
		context.define("number", isNumber());
		context.define("string", isString());
		context.define("boolean", isBoolean());

		
		return context;
	}

}
