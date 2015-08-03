package org.dykman.jtl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import static org.dykman.jtl.future.InstructionFutureFactory.*;

public class JtlCompiler {
	final JSONBuilder jsonBuilder;
	boolean trace;
	boolean profile;
	boolean imported;

	public JtlCompiler(JSONBuilder jsonBuilder) {
		this(jsonBuilder,false,false,false);
	}
	
	
	public JtlCompiler(JSONBuilder jsonBuilder,boolean trace, boolean profile,boolean imported) {
		this.jsonBuilder = jsonBuilder;
		this.trace= trace;
		this.profile = profile;
		this.imported = imported;
	}
	
	public InstructionFuture<JSON> parse(String src,InputStream in) 
		throws IOException {
		return parse(src,in, trace, profile);
	}
	
	public InstructionFuture<JSON> parse(String src,InputStream in,boolean trace,boolean profile) 
			throws IOException {
			return parse(src, new jtlLexer(new ANTLRInputStream(in)),trace,profile);
		}
	
	public InstructionFuture<JSON> parse(String src,File in,boolean trace,boolean profile) 
			throws IOException {
		return parse(src,new FileInputStream(in),trace,profile);
		}
	public InstructionFuture<JSON> parse(File in) 
		throws IOException {
		return parse(in.getName(),in, trace, profile);
	}

	public InstructionFuture<JSON> parse(String src,String in) 
		throws IOException {
		return parse(src,in, trace, profile);
	}
	
	public InstructionFuture<JSON> parse(String src,String in,boolean trace,boolean profile) 
			throws IOException {
			return parse(src, new jtlLexer(new ANTLRInputStream(in)),trace,profile);
		}
	protected InstructionFuture<JSON> parse(String src,jtlLexer lexer) 
		throws IOException {
		return parse(src,lexer, trace, profile);
	}
	
	protected InstructionFuture<JSON> parse(String file,jtlLexer lexer,boolean trace,boolean profile) 
			throws IOException {
	//	lexer.
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
			parser.setTrace(trace);
			parser.setProfile(profile);
//			parser.getCurrentToken();
			JtlContext tree = parser.jtl();
			InstructionFutureVisitor visitor = new InstructionFutureVisitor(file,jsonBuilder,imported);
			InstructionFutureValue<JSON> v = visitor.visit(tree);
			return v.inst;
		}

	
	
	
	public static SourceInfo getSource(String source,ParserRuleContext ctx,String name) {
      SourceInfo info = new SourceInfo();
      info.line = ctx.start.getLine();
      info.position = ctx.start.getCharPositionInLine();
      info.endline = ctx.stop.getLine();
      info.endposition = ctx.stop.getCharPositionInLine() + ctx.stop.getText().length();
      info.code = ctx.getText();
      info.source = source == null ? ctx.start.getTokenSource().getSourceName() : source;
      info.name = name;
      return info;
   }


   public static AsyncExecutionContext<JSON> createInitialContext(
			JSON data,
			JSON config,
			File f,
			JSONBuilder builder,
			ListeningExecutorService les ) {

      ListenableFuture<JSON> df = Futures.immediateCheckedFuture(data);
		SimpleExecutionContext context = new SimpleExecutionContext(builder,
		      df,config,f);
		context.setExecutionService(les);
		
		SourceInfo meta = new SourceInfo();
		meta.code = "*internal*";
		meta.source=meta.code;
		meta.line=meta.position=0;
		// configurable: import, extend
      context.define("_", InstructionFutureFactory.value(df,meta));
		if(config.getType() == JSONType.OBJECT) {
			JSONObject conf = (JSONObject) config;
			JSONObject modules= (JSONObject)conf.get("modules");
			context.define("module", loadModule(meta,modules));
			context.define("import", importInstruction(meta,config));
		}
		
		// general
		context.define("error", defaultError(meta));
		context.define("params", params(meta));

		// external data
		context.define("file", file(meta));
      context.define("url", url(meta));
      context.define("write", write(meta));

		
		// string-oriented
      context.define("split", split(meta));
      context.define("join", join(meta));
      context.define("substr", substr(meta));
		
		// list-oriented
		context.define("unique", unique(meta));
		context.define("count", count(meta));
		context.define("sort", sort(meta,false));
		context.define("rsort", sort(meta,true));
		context.define("filter", filter(meta));
      context.define("contains", contains(meta));
      context.define("copy", copy(meta));

		// object-oriented
		context.define("group", groupBy(meta));
		context.define("map", map(meta));
		context.define("collate", collate(meta));
		context.define("omap", omap(meta));
		context.define("amend", amend(meta));
      context.define("keys", keys(meta));

// ??		context.define("apply", apply());
	
		// boolean type test only
      context.define("null", isNull(meta));
      context.define("value", isValue(meta));
		context.define("object", isObject(meta));

		// with 0 args, they return boolean type test
		// with 1 arg, attempts to coerce to the specified type
		context.define("array", isArray(meta));
		context.define("number", isNumber(meta));
		context.define("string", isString(meta));
		context.define("boolean", isBoolean(meta));

		
		return context;
	}

}
