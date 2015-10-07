package org.dykman.jtl;

import java.io.FileInputStream;
import java.io.File;
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
		this(jsonBuilder, false, false, false);
	}

	public JtlCompiler(JSONBuilder jsonBuilder, boolean trace, boolean profile, boolean imported) {
		this.jsonBuilder = jsonBuilder;
		this.trace = trace;
		this.profile = profile;
		this.imported = imported;
	}

	public InstructionFuture<JSON> parse(String src, InputStream in) throws IOException {
		return parse(src, in, trace, profile);
	}

	public InstructionFuture<JSON> parse(String src, InputStream in, boolean trace, boolean profile)
			throws IOException {
		return parse(src, new jtlLexer(new ANTLRInputStream(in)), trace, profile);
	}

	public InstructionFuture<JSON> parse(String src, File in, boolean trace, boolean profile) throws IOException {
		return parse(src, new FileInputStream(in), trace, profile);
	}

	public InstructionFuture<JSON> parse(File in) throws IOException {
		// System.err.println("parsing file: " + in.getAbsolutePath());
		return parse(in.getName(), in, trace, profile);
	}

	public InstructionFuture<JSON> parse(String src, String in) throws IOException {
		return parse(src, in, trace, profile);
	}

	public InstructionFuture<JSON> parse(String src, String in, boolean trace, boolean profile) throws IOException {
		return parse(src, new jtlLexer(new ANTLRInputStream(in)), trace, profile);
	}

	protected InstructionFuture<JSON> parse(String src, jtlLexer lexer) throws IOException {
		return parse(src, lexer, trace, profile);
	}

	protected InstructionFuture<JSON> parse(String file, jtlLexer lexer, boolean trace, boolean profile)
			throws IOException {
		// lexer.
		jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
		parser.setTrace(trace);
		parser.setProfile(profile);
		// parser.getCurrentToken();
		JtlContext tree = parser.jtl();
		InstructionFutureVisitor visitor = new InstructionFutureVisitor(file, jsonBuilder, imported);
		InstructionFutureValue<JSON> v = visitor.visit(tree);
		return InstructionFutureFactory.fixContextData(v.inst);
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

	public static void define(AsyncExecutionContext<JSON> ctx, String s, InstructionFuture<JSON> inst) {
		ctx.define(s, inst);
	}

	public static AsyncExecutionContext<JSON> createInitialContext(JSON data, JSONObject config, File f, JSONBuilder builder,
			ListeningExecutorService les) {

		ListenableFuture<JSON> df = Futures.immediateCheckedFuture(data);
		SimpleExecutionContext context = new SimpleExecutionContext(builder, df, config, f);
		context.setExecutionService(les);

		SourceInfo meta = new SourceInfo();
		meta.code = "*internal*";
		meta.source = meta.code;
		meta.line = meta.position = 0;
		// configurable: import, extend
		define(context, "_", InstructionFutureFactory.value(df, meta));

		define(context, "module", loadModule(meta, config));
		define(context, "import", importInstruction(meta, config));

		// general
		define(context, "error", defaultError(meta));
		define(context, "params", params(meta));
		define(context, "rand", rand(meta));
		define(context, "switch", switchInst(meta));
		define(context, "each", each(meta));
		define(context, "defined", defined(meta));
		define(context, "call", call(meta));
		define(context, "thread", thread(meta));

		// external data
		define(context, "file", file(meta));
		define(context, "url", url(meta));
		define(context, "write", write(meta));

		// string-oriented
		define(context, "split", split(meta));
		define(context, "join", join(meta));
		define(context, "substr", substr(meta));
		define(context, "sprintf", sprintf(meta));

		// list-oriented
		define(context, "unique", unique(meta));
		define(context, "count", count(meta));
		define(context, "sort", sort(meta, false));
		define(context, "rsort", sort(meta, true));
		define(context, "filter", filter(meta));
		define(context, "contains", contains(meta));
		define(context, "copy", copy(meta));
		define(context, "append", append(meta));
		define(context, "sum", sum(meta));
		define(context, "min", min(meta));
		define(context, "max", max(meta));
		define(context, "avg", avg(meta));

		// object-oriented
		define(context, "group", groupBy(meta));
		define(context, "map", map(meta));
		define(context, "collate", collate(meta));
		define(context, "omap", omap(meta));
		define(context, "amend", amend(meta));
		define(context, "keys", keys(meta));

		// ?? define("apply", apply());

		// 0 args: return boolean type test
		// 1 arg: attempts to coerce to the specified type
		define(context, "array", isArray(meta));
		define(context, "number", isNumber(meta));
		define(context, "int", isInt(meta));
		define(context, "real", isReal(meta));
		define(context, "string", isString(meta));
		define(context, "boolean", isBoolean(meta));

		// 0 args: boolean type test only
		define(context, "nil", isNull(meta));
		define(context, "value", isValue(meta));
		define(context, "object", isObject(meta));

		return context;
	}

}
