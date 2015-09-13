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
		final File config;
		final File init;
		final File defString;
		final JSONObject baseConfig;
		AsyncExecutionContext<JSON> initializedContext = null;
		final Map<String, InstructionFuture<JSON>> programs = new HashMap<>();

		static JtlExecutor theInstance = null;

		public static JtlExecutor getInstance(
				File base, // jtl install directory, required
				File serverBase, // server root, required 
				File config, // optional config, may be null
				File init, // optional init, may be null
				File defScript, boolean canonical) 
		 throws IOException, ServletException {
			return new JtlExecutor(base, serverBase, config, init, defScript, canonical);
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

		
		public JtlExecutor(
				File base, // jtl install directory, required
				File serverBase, // server root, required 
				File config, // optional config, may be null
				File init, // optional init, may be null
				File defScript, boolean canonical) 
		throws IOException, ServletException {
			this.jtlBase = base;
			this.serverBase = serverBase;
			this.config = config;
			this.init = init;
			this.defString = defScript;
			this.builder = new JSONBuilderImpl(canonical);
			this.compiler = new JtlCompiler(builder, false, false, false);
			JSONObject bc = (JSONObject) builder.parse(new File(jtlBase, "conf/config.json"));
			if(config != null) {
				if (!config.exists()) {
					throw new ServletException("unable to locate specified config: " + config.getAbsolutePath());
				}
				bc = bc.overlay((JSONObject) builder.parse(config));
			}
			baseConfig = bc;
			
		}
			
		
		JSON execute( 
				HttpServletRequest req,
				HttpServletResponse res) 
			throws ExecutionException {
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

		JtlExecutor jtlExecutor = null;
		public JtlServlet() {
			this.builder = new JSONBuilderImpl();
			this.compiler = new JtlCompiler(builder, false, false, false);
		}


		@Override
		public void init(ServletConfig config) throws ServletException {
			try {
				String s = config.getInitParameter("jtlbase");
				File jtlRoot = new File(s);

				s = config.getInitParameter("root");
				File serverRoot = new File(s);
				
				s = config.getInitParameter("config");
				File conf = s == null ? null : new File(s);

				s = config.getInitParameter("script");
				File defScript = s == null ? null : new File(s);

				s = config.getInitParameter("init");
				File init = s == null ? null : new File(s);

				s = config.getInitParameter("canonical");
				boolean canon = s == null ? false : s.equalsIgnoreCase("true");

				jtlExecutor = JtlExecutor.getInstance(jtlRoot, serverRoot, conf, init, defScript, canon);
			} catch (IOException e) {
				throw new ServletException(e);
			}
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
				try {
					JSON r = jtlExecutor.execute(req, resp);
					int indent = ss == null ? 0 : Integer.parseInt(ss);
					r.write(resp.getWriter(), indent, false);
					resp.getWriter().flush();
				} catch (ExecutionException  e) {
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
