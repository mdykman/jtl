package org.dykman.jtl.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import static org.dykman.jtl.future.InstructionFutureFactory.*;

public class JtlServer {

	final Server server;
	final File base;
	final File script;
	JSONBuilder builder = new JSONBuilderImpl();

	File servletRoot = null;
	File init = null;
	InstructionFuture<JSON> defaultProgram = null;

	ListeningExecutorService les;
	JSON config;
	int port;

	public JtlServer(File base, File init, File script, JSON config, ListeningExecutorService les, int port) {
		this.les = les;
		this.base = base;
		this.script = script;
		this.port = port;
		this.config = config;
		server = new Server(7719);

		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(JtlServlet.class, "/*");
		server.setHandler(handler);
	}

	public void start() throws Exception {
		if (base.isDirectory()) {
			servletRoot = base;
		} else {
			servletRoot = base.getParentFile();
			JtlCompiler compiler = new JtlCompiler(builder);
			defaultProgram = compiler.parse(base);
		}

		server.start();
	}

	public void join() throws InterruptedException {
		server.join();
	}

	public static class JtlExecutor {
		final JSONBuilder builder;
		final JtlCompiler compiler;
		final File serverBase;
		final File jtlBase;
		AsyncExecutionContext<JSON> initializedContext = null;
		final Map<String, InstructionFuture<JSON>> programs = new HashMap<>();

		public JtlExecutor(File base,File\fq) {
			this.jtlBase = base;
			this.builder = new JSONBuilderImpl();
			this.compiler = new JtlCompiler(builder, false, false, false);
		}
		JSON execute(String uri) {
			JSON r = null;
			
			return r;
		}
	}

	public static class JtlServlet extends HttpServlet {

		final JSONBuilder builder;
		final JtlCompiler compiler;
		File serverRoot = null;
		File jtlRoot = null;
		JSONObject config = null;
		InstructionFuture<JSON> defInst = null;
		InstructionFuture<JSON> initInst = null;
		AsyncExecutionContext<JSON> initContext;

		public JtlServlet() {
			this.builder = new JSONBuilderImpl();
			this.compiler = new JtlCompiler(builder, false, false, false);
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

		@Override
		public void init(ServletConfig config) throws ServletException {
			try {
				String s = config.getInitParameter("jtlbase");
				jtlRoot = new File(s);
				s = config.getInitParameter("root");
				serverRoot = new File(s);
				JSONObject baseConfig = (JSONObject) builder.parse(new File(jtlRoot, "conf/config.json"));
				s = config.getInitParameter("config");
				if (s != null) {
					File f = new File(s);
					if (!f.exists()) {
						f = new File(serverRoot, s);
					}
					if (!f.exists()) {
						throw new ServletException("unable to locate specified config: " + s);
					}
					baseConfig = baseConfig.overlay((JSONObject) builder.parse(new File(s)));
				}
				this.config = baseConfig;

				s = config.getInitParameter("script");
				if (s != null) {
					File f = new File(s);
					if (!f.exists()) {
						f = new File(serverRoot, s);
					}
					if (!f.exists()) {
						throw new ServletException("unable to locate specified config: " + s);
					}
					defInst = compiler.parse(f);
				}

				AsyncExecutionContext<JSON> context = JtlCompiler.createInitialContext(this.config, this.config,
						serverRoot, builder, getExecutorService());

				s = config.getInitParameter("init");
				if (s != null) {
					ListenableFuture<JSON> asConf = Futures.immediateCheckedFuture(this.config);
					initContext = context.createChild(false, false, asConf, null);
					initInst = compiler.parse(new File(s));
					initInst.call(initContext, asConf);
				}

			} catch (IOException e) {
				throw new ServletException(e);
			} catch (ExecutionException e) {
				throw new ServletException(e.report(), e);
			}
		}

		protected AsyncExecutionContext<JSON> servletContext(HttpServletRequest req, JSON data) {
			// Pair<String,Integer> meta = new Pair<String,
			// Integer>("http service", 0);
			SourceInfo meta = new SourceInfo();
			meta.source = meta.code = "http service";
			meta.position = meta.line = 0;
			String[] pp = req.getPathInfo().split("[/]");
			context.define("0", value(base.getPath(), builder, meta));
			for (int i = 1; i < pp.length; ++i) {
				context.define(Integer.toString(i), value(pp[i], builder, meta));
			}
			JSONObject object = builder.object(null);
			for (Map.Entry<String, String[]> el : req.getParameterMap().entrySet()) {
				JSONArray arr = builder.array(object);
				for (String sv : el.getValue()) {
					arr.add(builder.value(sv));
				}
				object.put(el.getKey(), arr);
			}
			context.define("params", value(object, meta));

			object = builder.object(null);
			Enumeration<String> hk = req.getHeaderNames();
			while (hk.hasMoreElements()) {
				String k = hk.nextElement();
				object.put(k, builder.value(req.getHeader(k)));
			}
			context.define("headers", value(object, meta));
			return context;
		}

		JSONObject config() throws IOException {
			File f = new File(jtlRoot, "etc/config.json");
			JSONObject obj = null;
			if (f.exists()) {
				JSON j = builder.parse(f);
				if (!(j instanceof JSONObject)) {

				} else {
					obj = (JSONObject) j;
				}

			}

			return obj;
		}

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			if (req.getMethod().equalsIgnoreCase("post")) {
				String ss = req.getParameter("indent");
				JSON data = builder.parse(req.getInputStream());
				AsyncExecutionContext<JSON> context = servletContext(req, data);
				try {
					if (defInst != null) {
						ListenableFuture<JSON> j = defInst.call(context, Futures.immediateFuture(data));
						int indent = ss == null ? 0 : Integer.parseInt(ss);
						j.get().write(resp.getWriter(), indent, false);
						resp.getWriter().flush();
					} else {
						req.getRequestURI().split("[/]");
						reportError(404, "dynamic loading not yet implemented", resp);
					}
				} catch (ExecutionException | InterruptedException | java.util.concurrent.ExecutionException e) {
					reportError(500, e.getLocalizedMessage(), resp);
				}

			}
		}

		protected void reportError(int code, String message, HttpServletResponse resp) {
			resp.setStatus(code);
			try {
				resp.getWriter().println(message);
				resp.getWriter().flush();
			} catch (IOException e) {
				System.err.println("bailed during error: " + e.getLocalizedMessage());
			}
		}

	}
}
