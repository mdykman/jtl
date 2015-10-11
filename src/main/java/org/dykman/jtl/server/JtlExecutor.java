package org.dykman.jtl.server;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class JtlExecutor {
	final JSONBuilder builder;
	final JtlCompiler compiler;
	final File serverBase;
	final File jtlBase;
	final File config;
	final File init;
	final File boundScript;

	InstructionFuture<JSON> boundInst = null;
	AsyncExecutionContext<JSON> boundContext = null;
	ListenableFuture<JSON> configFuture;

	final String dirDefault;
	final JSONObject baseConfig;
	AsyncExecutionContext<JSON> initializedContext = null;
	ListenableFuture<JSON> initResult;

	final Map<String, InstructionFuture<JSON>> programs = new ConcurrentHashMap<>();
	final Map<String, AsyncExecutionContext<JSON>> contexts = new ConcurrentHashMap<>();
	final Map<String, Long> lastModified = new ConcurrentHashMap<>();
	// ListeningExecutorService les =
	// MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

	static JtlExecutor theInstance = null;
	
	static Map<String,File> filelocks = new HashMap<>();
	static synchronized File lockingFile(String path) {
		File f = filelocks.get(path);
		if(f == null) {
			f = new File(path);
			filelocks.put(path, f);
		}
		return f;		
	}
	static synchronized File lockingFile(File fin) {
		String p = fin.getPath();
		File f = filelocks.get(p);
		if(f == null) {
			filelocks.put(p, fin);
			f = fin;
		}
		return f;		
	}

	public static JtlExecutor getInstance(File base, // jtl install directory,
														// required
			File serverBase, // server root, required
			File config, // optional config, may be null
			File init, // optional init, may be null
			File boundScript, // optional default script , may be null
			String dirDefault, boolean canonical) throws IOException, ServletException {
		return new JtlExecutor(base, serverBase, config, init, boundScript, dirDefault, canonical);
	}

	static ListeningExecutorService executorService = null;

	static ListeningExecutorService getExecutorService() {
		if (executorService == null) {
			synchronized (JtlServer.class) {
				if (executorService == null) {
					executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

				}
			}
		}
		return executorService;
	}

	public JtlExecutor(File base, // jtl install directory, required
			File serverBase, // server root, required
			File config, // optional config, may be null
			File init, // optional init, may be null
			File boundScript, // optional boundScript, may be null
			String dirDefault, // ie. 'default.json' = only needed if
								// boundScript == null
			boolean canonical) throws IOException, ServletException {
		this.jtlBase = base;
		this.serverBase = serverBase;
		this.config = config;
		this.init = init;
		this.boundScript = boundScript;
		this.dirDefault = dirDefault;
		this.builder = new JSONBuilderImpl(canonical);
		this.compiler = new JtlCompiler(builder);
		JSONObject bc = (JSONObject) builder.parse(new File(jtlBase, "conf/config.json"));

		if (config != null) {
			if (!config.exists()) {
				throw new ServletException("unable to locate specified config: " + config.getAbsolutePath());
			}
			bc = bc.overlay((JSONObject) builder.parse(config));
		}
		bc.put("server-mode", builder.value(true));
		baseConfig = bc;
		configFuture = Futures.immediateCheckedFuture(baseConfig);
		if (boundScript != null)
			boundInst = compiler.parse(boundScript);
	}

	public void cleanUp() {
		for (AsyncExecutionContext<JSON> cc : contexts.values()) {
			cc.cleanUp();
		}
	}

	public JSON executeScript(HttpServletRequest req, HttpServletResponse res, AsyncExecutionContext<JSON> baseContext,
			File execFile, InstructionFuture<JSON> prog, String selector, String[] path, JSON data)
					throws IOException, ExecutionException {
		AsyncExecutionContext<JSON> ctx = httpContext(req, res, baseContext, execFile, selector, path);
		ctx.define("_", InstructionFutureFactory.value(data, SourceInfo.internal("http")));
		try {
			// TODO:: remove assert
			JSON j = prog.call(ctx, Futures.immediateCheckedFuture(data)).get();
			return j;
		} catch (java.util.concurrent.ExecutionException | InterruptedException e) {
			throw new ExecutionException(e, SourceInfo.internal("http"));
		} finally {
			ctx.getRuntime().cleanUp();
		}
	}

	public JSON initScript(HttpServletRequest req, HttpServletResponse res, AsyncExecutionContext<JSON> baseContext,
			File execFile, InstructionFuture<JSON> prog,String selector, String[] path, JSON data, int cc)
					throws IOException, ExecutionException {
		execFile = lockingFile(execFile);
//		InstructionFuture<JSON> prog = programs.get(p);
		if(prog == null) synchronized (execFile) {
			final  String p = execFile.getPath();
			prog = programs.get(p);
			if (prog == null) {
				prog = compiler.parse(execFile);
				// per-script init context
				AsyncExecutionContext<JSON> ctx = baseContext.createChild(false, false, null, null);
				ctx.setInit(true);
				JSON j = executeScript(req, res, ctx, execFile, prog, selector, path, data);
				contexts.put(p, ctx);
				programs.put(p, prog);
				lastModified.put(p, execFile.lastModified());
				return j;
			}
		}
		return null;
	}

	public JSON tryFile(HttpServletRequest req, HttpServletResponse res, AsyncExecutionContext<JSON> baseContext,
			File execFile, JSON data, String[] parts, int cc) throws IOException, ExecutionException {
		JSON j = null;
		if (execFile.exists()) {
			String p = execFile.getPath();
			InstructionFuture<JSON> prog = null;
			if(lastModified.containsKey(p) && (execFile.lastModified() == lastModified.get(p))) {
				prog = programs.get(execFile.getPath());
			} else {
				programs.remove(p);
			}
			String sel = String.join("/", Arrays.copyOfRange(parts, 0, cc + 1));
			String[] path = Arrays.copyOfRange(parts, cc + 1, parts.length);
			if (prog == null) {
				j = initScript(req, res, baseContext, execFile, prog, sel, path, data, cc);
				if (j != null)
					return j;
				prog = programs.get(execFile.getPath());;
			}
			AsyncExecutionContext<JSON> ctx = contexts.get(execFile.getPath());
			return executeScript(req, res, ctx, execFile, prog, sel, path, data);

		}
		return null;
	}

	public JSON preExec(HttpServletRequest req, HttpServletResponse res, AsyncExecutionContext<JSON> baseContext,
			JSON data) throws IOException, ExecutionException {
		JSON j = null;
		String uri = req.getRequestURI();
		String[] parts = uri.substring(1).split("[/]");
		if (boundScript != null) {
			if (boundContext == null) {
				synchronized (this) {
					if (boundContext == null) {
						boundContext = baseContext.createChild(false, false, null, null);
						boundContext.setInit(true);
					}
					return executeScript(req, res, boundContext, this.boundScript, boundInst, "/", parts, data);
				}
			} else {
				return executeScript(req, res, boundContext, this.boundScript, boundInst, "/", parts, data);
			}
		} else {
			File bb = serverBase;
			int cc = 0;
			for (String s : parts) {
				String ss = s + ".jtl";
				File f = new File(bb, ss);
				j = tryFile(req, res, baseContext, f, data, parts, cc);
				if (j != null)
					return j;
				bb = new File(bb, ss);
				if (bb.isDirectory()) {
					f = new File(bb, this.dirDefault);
					j = tryFile(req, res, baseContext, f, data, parts, cc);
					if (j != null)
						return j;
					// else return notfound(uri);
					++cc;
				} else {
					return notfound(uri);
				}
			}
			return notfound(uri);

		}
	}

	public JSON notfound(String path) {
		JSONObject obj = builder.object(null);
		obj.put("message", builder.value("no resource found named " + path));
		return obj;
	}

	public JSON execute(HttpServletRequest req, HttpServletResponse res, JSON data)
			throws ExecutionException, IOException {
		// global server init block
		if (initializedContext == null) {
			synchronized (this) {
				if (initializedContext == null) {
					initializedContext = JtlCompiler.createInitialContext(baseConfig, baseConfig,
							serverBase, builder, getExecutorService());
					initializedContext.setInit(true);
					if (init != null) {
						InstructionFuture<JSON> initf = compiler.parse(init);
						initResult = initf.call(initializedContext, configFuture);
					}
					return preExec(req, res, initializedContext, data);
				}
			}
		}
		return preExec(req, res, initializedContext, data);
	}

	public AsyncExecutionContext<JSON> httpContext(HttpServletRequest req, HttpServletResponse res,
			AsyncExecutionContext<JSON> parent, File execFile, String selector, String[] path) throws IOException {
		AsyncExecutionContext<JSON> ctx = parent.createChild(false, false, null, null);
		ctx.setRuntime(true);
		// map the request arguments
		JSONObject jrq = builder.object(null);
		for (Map.Entry<String, String[]> kk : req.getParameterMap().entrySet()) {
			JSONArray arr = builder.array(jrq);
			for (String v : kk.getValue()) {
				arr.add(builder.value(v));
			}
			jrq.put(kk.getKey().replaceAll("[-]", "_"), arr);
		}
		ctx.define("req", InstructionFutureFactory.value(jrq, SourceInfo.internal("http")));

		// map the request headers
		Enumeration<String> en = req.getHeaderNames();
		jrq = builder.object(null);

		while (en.hasMoreElements()) {
			String k = en.nextElement();
			jrq.put(k.replaceAll("[-]", "_"), builder.value(req.getHeader(k)));
		}
		ctx.define("headers", InstructionFutureFactory.value(jrq, SourceInfo.internal("http")));

		// path arguments
		ctx.define("0", InstructionFutureFactory.value(builder.value(execFile.getCanonicalPath()),
				SourceInfo.internal("http")));
		int cc = 1;
		JSONArray arr = builder.array(null);
		for (String s : path) {
			JSON v = builder.value(s);
			ctx.define(Integer.toString(cc++), InstructionFutureFactory.value(v, SourceInfo.internal("http")));
			arr.add(v);
		}
		ctx.define("@", InstructionFutureFactory.value(arr, SourceInfo.internal("http")));
//		ctx.define("#", InstructionFutureFactory.value(builder.value(arr.size()), SourceInfo.internal("http")));
		/// ctx.define("_",
		/// InstructionFutureFactory.value(builder.value(arr.size()), null));
		// req.getp
		// misc. metadata
		ctx.define("selector", InstructionFutureFactory.value(builder.value(selector), SourceInfo.internal("http")));
		ctx.define("uri",
				InstructionFutureFactory.value(builder.value(req.getRequestURI()), SourceInfo.internal("http")));
		ctx.define("url", InstructionFutureFactory.value(builder.value(req.getRequestURL().toString()),
				SourceInfo.internal("http")));
		ctx.define("method",
				InstructionFutureFactory.value(builder.value(req.getMethod()), SourceInfo.internal("http")));
		return ctx;
	}

}