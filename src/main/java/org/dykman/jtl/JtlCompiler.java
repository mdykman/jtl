package org.dykman.jtl;

import java.io.FileInputStream;

import static org.dykman.jtl.operator.FutureInstructionFactory.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRErrorStrategy;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.dykman.jtl.jtlLexer;
import org.dykman.jtl.jtlParser;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.jtlParser.JtlContext;
import org.dykman.jtl.operator.FutureInstruction;
import org.dykman.jtl.operator.FutureInstructionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.FutureInstructionValue;
import org.dykman.jtl.future.FutureInstructionVisitor;
import org.dykman.jtl.future.SimpleExecutionContext;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class JtlCompiler {
	
	static Logger logger = LoggerFactory.getLogger(JtlCompiler.class);

	final JSONBuilder jsonBuilder;
	boolean trace;
	boolean profile;
	boolean imported;
	
	public JtlCompiler(JSONBuilder jsonBuilder) {
		this(jsonBuilder, false, false, false);
	}

	private JtlCompiler(JSONBuilder jsonBuilder, boolean trace, boolean profile, boolean imported) {
		this.jsonBuilder = jsonBuilder;
		this.trace = trace;
		this.profile = profile;
		this.imported = imported;
	}

	public FutureInstruction<JSON> parse(String src, InputStream in) throws IOException {
		return parse(src, in, trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, InputStream in, boolean trace, boolean profile)
			throws IOException {
		return parse(src, new jtlLexer(new ANTLRInputStream(in)), trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, File in, boolean trace, boolean profile) throws IOException {
		return parse(src, new FileInputStream(in), trace, profile);
	}

	public FutureInstruction<JSON> parse(File in) throws IOException {
		// System.err.println("parsing file: " + in.getAbsolutePath());
		return parse(in.getName(), in, trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, String in) throws IOException {
		return parse(src, in, trace, profile);
	}

	public FutureInstruction<JSON> parse(String src, String in, boolean trace, boolean profile) throws IOException {
		return parse(src, new jtlLexer(new ANTLRInputStream(in)), trace, profile);
	}

	protected FutureInstruction<JSON> parse(String src, jtlLexer lexer) throws IOException {
		return parse(src, lexer, trace, profile);
	}

	protected FutureInstruction<JSON> parse(
			String file, jtlLexer lexer, boolean trace, boolean profile)
			throws IOException {
		jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
		BailErrorStrategy bstrat = new BailErrorStrategy() {
			@Override
			public void reportError(Parser recognizer, RecognitionException re) {
System.err.println("ERROR HANDLER CALLED");
				RuleContext rc = re.getCtx();
				IntervalSet is = re.getExpectedTokens();
				System.err.println("error text: " + rc.getText());
				for(Interval iv  : is.getIntervals()) {
				//	iv.
				}
			}
			@Override
			public void reportInputMismatch(Parser recognizer, InputMismatchException ime) {
				System.err.println("MISMATCH HANDLER CALLED");
				RuleContext rc = ime.getCtx();
				System.err.println("error text: " + rc.getText());
				ime.getExpectedTokens();
			}
		    @Override
		    public Token recoverInline(Parser recognizer)
		        throws RecognitionException
		    {
				InputMismatchException e = new InputMismatchException(recognizer);
				for (ParserRuleContext context = recognizer.getContext(); context != null; context = context.getParent()) {
					context.exception = e;
				}
				Token t = recognizer.getCurrentToken();
				throw new JtlParseException(t);
				/*
				System.err.print("error : " + t.getLine() + ':' + t.getStartIndex());
				System.err.println(t.getText());
				System.err.flush();
		        throw new ParseCancellationException(e);
		        */
		    }


		};
		parser.setErrorHandler(bstrat);
		parser.setTrace(trace);
		parser.setProfile(profile);
		try {
			JtlContext tree = parser.jtl();
//			RecognitionException re = tree.exception;
			FutureInstructionVisitor visitor = 
					new FutureInstructionVisitor(file, jsonBuilder, imported);
			FutureInstructionValue<JSON> v = visitor.visit(tree);
			return FutureInstructionFactory.fixContextData(v.inst);
//		} catch(ParseCancellationException e) {
		} catch(JtlParseException e) {
			logger.error(e.report(),e);
			return FutureInstructionFactory.value(jsonBuilder.value(), 
					SourceInfo.internal("parser"));
		} catch(Exception e) {
			logger.error(e.getLocalizedMessage(),e);
			return FutureInstructionFactory.value(jsonBuilder.value(), 
					SourceInfo.internal("parser"));
		}
	}

	public static SourceInfo getSource(String source, ParserRuleContext ctx, String name) {
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

	public JSONBuilder getJsonBuilder() {
		return jsonBuilder;
	}

	public static void define(AsyncExecutionContext<JSON> ctx, String s, FutureInstruction<JSON> inst) {
		ctx.define(s, inst);
	}

	
	public AsyncExecutionContext<JSON> createInitialContext(
			JSON data, JSONObject config, File scriptBase, File init,
			JSONBuilder builder, ListeningExecutorService les) throws IOException, ExecutionException {

		ListenableFuture<JSON> df = Futures.immediateCheckedFuture(data);
		SimpleExecutionContext context = new SimpleExecutionContext(builder, df, config, scriptBase);
		context.setExecutionService(les);

		SourceInfo meta = new SourceInfo();
		meta.code = "*internal*";
		meta.source = meta.code;
		meta.line = meta.position = 0;
		// configurable: import, extend
		define(context, "_", FutureInstructionFactory.value(df, SourceInfo.internal("input")));

		define(context, "module", loadModule(SourceInfo.internal("module"), config));
		define(context, "import", importInstruction(SourceInfo.internal("import"), config));

		
		define(context, "try", attempt(SourceInfo.internal("try")));

		// general
		define(context, "error", defaultError(SourceInfo.internal("error")));
		define(context, "params", params(SourceInfo.internal("params")));
		define(context, "rand", rand(SourceInfo.internal("rand")));
		define(context, "switch", switchInst(SourceInfo.internal("switch")));
		define(context, "each", each(SourceInfo.internal("each")));
		define(context, "defined", defined(SourceInfo.internal("defined")));
		define(context, "call", call(SourceInfo.internal("call")));
		define(context, "thread", thread(SourceInfo.internal("thread")));
		define(context, "type", type(SourceInfo.internal("type")));
		define(context, "trace", trace(SourceInfo.internal("trace")));
		define(context, "log", log(SourceInfo.internal("trace")));
		define(context, "digest", digest(SourceInfo.internal("digest")));

		// external data
		define(context, "file", file(SourceInfo.internal("file")));
		define(context, "url", url(SourceInfo.internal("url")));
		define(context, "write", write(SourceInfo.internal("write")));
		define(context, "mkdirs", mkdirs(SourceInfo.internal("nkdirs")));
		define(context, "fexists", fexists(SourceInfo.internal("fexists")));
			
		// string-oriented
		define(context, "split", split(SourceInfo.internal("split")));
		define(context, "join", join(SourceInfo.internal("join")));
		define(context, "substr", substr(SourceInfo.internal("substr")));
		define(context, "sprintf", sprintf(SourceInfo.internal("sprintf")));
		define(context, "upper", tocase(SourceInfo.internal("sprintf"),true));
		define(context, "lower", tocase(SourceInfo.internal("sprintf"),false));
		

		// list-oriented
		define(context, "unique", unique(SourceInfo.internal("unique")));
		define(context, "count", count(SourceInfo.internal("count")));
		define(context, "sort", sort(SourceInfo.internal("sort"), false));
		define(context, "rsort", sort(SourceInfo.internal("rsort"), true));
		define(context, "filter", filter(SourceInfo.internal("filter")));
		define(context, "contains", contains(SourceInfo.internal("contains")));
		define(context, "copy", copy(SourceInfo.internal("copy")));
		define(context, "append", append(SourceInfo.internal("append")));
		define(context, "sum", sum(SourceInfo.internal("sum")));
		define(context, "min", min(SourceInfo.internal("min")));
		define(context, "max", max(SourceInfo.internal("max")));
		define(context, "avg", avg(SourceInfo.internal("avg")));
		define(context, "pivot", pivot(SourceInfo.internal("pivot")));

		// object-oriented
		define(context, "group", groupBy(SourceInfo.internal("group")));
		define(context, "map", map(SourceInfo.internal("map")));
		define(context, "collate", collate(SourceInfo.internal("collate")));
		define(context, "omap", omap(SourceInfo.internal("omap")));
		define(context, "amend", amend(SourceInfo.internal("amend")));
		define(context, "keys", keys(SourceInfo.internal("keys")));
		define(context, "rekey", rekey(SourceInfo.internal("rekey")));


		// 0 args: return boolean type test
		// 1 arg: attempts to coerce to the specified type
		define(context, "array", isArray(SourceInfo.internal("array")));
		define(context, "number", isNumber(SourceInfo.internal("number")));
		define(context, "int", isInt(SourceInfo.internal("int")));
		define(context, "real", isReal(SourceInfo.internal("real")));
		define(context, "string", isString(SourceInfo.internal("string")));
		define(context, "boolean", isBoolean(SourceInfo.internal("boolean")));

		// 0 args: boolean type test only
		define(context, "nil", isNull(SourceInfo.internal("nil")));
		define(context, "value", isValue(SourceInfo.internal("value")));
		define(context, "object", isObject(SourceInfo.internal("object")));

		AsyncExecutionContext<JSON> ctx = context;
		if (init != null) {
			ctx = ctx.createChild(false, false, df, SourceInfo.internal("compiler"));
			ctx.setInit(true);
			FutureInstruction<JSON> initf = parse(init);
			ListenableFuture<JSON> initResult = initf.call(ctx, Futures.immediateCheckedFuture(config));
			ctx.define("init", FutureInstructionFactory.value(initResult, SourceInfo.internal("init")));
		}
		ctx.setInit(true);
		return ctx;
	}
}
