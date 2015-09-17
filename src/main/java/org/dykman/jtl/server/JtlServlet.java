package org.dykman.jtl.server;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;

public class JtlServlet extends HttpServlet {

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

			jtlExecutor = JtlExecutor.getInstance(jtlRoot, serverRoot, conf, init, defScript,"default.json", canon);
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
				JSON data = builder.parse(req.getInputStream());
				JSON r = jtlExecutor.execute(req, resp,data);
				int indent = ss == null ? 0 : Integer.parseInt(ss);
				r.write(resp.getWriter(), indent, true);
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