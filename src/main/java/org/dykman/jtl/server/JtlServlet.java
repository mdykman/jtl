package org.dykman.jtl.server;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.JtlMain;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.operator.FutureInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

@SuppressWarnings("serial")
public class JtlServlet extends HttpServlet {

	final JSONBuilder builder;
	final JtlCompiler compiler;
	File serverRoot = null;
	File jtlRoot = null;
	File resources = null;
//	JSONObject config = null;
	FutureInstruction<JSON> defInst = null;
	FutureInstruction<JSON> initInst = null;
	AsyncExecutionContext<JSON> initContext;
	
	Logger logger =
			LoggerFactory.getLogger(JtlServlet.class);

	JtlExecutor jtlExecutor = null;

	public JtlServlet() {
		this.builder = new JSONBuilderImpl();
		this.compiler = new JtlCompiler(builder);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		try {
			String s = config.getInitParameter("jtlbase");
			if(s == null) {	throw new ServletException("no JTL base specified"); }
			jtlRoot = new File(s);
			
			s = config.getInitParameter("root");
			serverRoot = new File(s);
			if(s == null) {	throw new ServletException("no server base specified"); }

			s = config.getInitParameter("config");
			File conf = s == null ? null : new File(s);

			s = config.getInitParameter("resources");
			resources = s == null ? null : new File(s);

			
			s = config.getInitParameter("script");
			File defScript = s == null ? null : new File(s);

			s = config.getInitParameter("init");
			File init = s == null ? null : new File(s);

			s = config.getInitParameter("canonical");
			boolean canon = "true".equalsIgnoreCase(s);

			jtlExecutor = JtlExecutor.getInstance(jtlRoot, serverRoot, conf, init, defScript, "default.json", canon);
			
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

	protected JSON parseData(HttpServletRequest req) throws IOException, ExecutionException {
		JSON data;
		switch (req.getMethod()) {
		case "POST":
		case "PUT":
		case "PATCH": {
			String type = req.getContentType();
			MediaType basetype = MediaType.parse(type);

			if (basetype.is(MediaType.JSON_UTF_8) || basetype.is(MediaType.ANY_TEXT_TYPE) || "application/json".equals(type) ||   "text/json".equals(type)) {
				Reader reader = req.getReader();
				data = builder.parse(reader);
			} else if ("application/x-www-form-urlencoded".equals(type)) {
				JSONObject obj = builder.object(null);
				for (Map.Entry<String, String[]> pp : req.getParameterMap().entrySet()) {
					JSONArray arr = builder.array(obj);
					for (String s : pp.getValue()) {
						arr.add(builder.value(s));
					}
					obj.put(pp.getKey(), arr);
				}
				data = obj;
			} else {
				throw new ExecutionException("don't know how to deal with content-type " + type, null);
			}
			break;
		}
		default:
			data = JSONBuilderImpl.NULL;
		}
		return data;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws  IOException , ServletException
	{

		String path = req.getRequestURI();
		if(resources != null && path!=null && path.length() > 0) {
			File f = new File(resources,path.substring(1));
			logger.debug(String.format("resources: testing for file %s",f.getCanonicalFile()));
			if(f.exists()) {
				logger.debug(String.format("resources: file %s found",f.getCanonicalFile()));
				req.getRequestDispatcher("/resources" + path).forward(req, resp);
				return;
			} else {
				logger.debug(String.format("resources: file %s not found",f.getCanonicalFile()));
			}
		}
		String ss = req.getParameter("indent");
		try {
			JSON r = jtlExecutor.execute(req, resp, parseData(req));
			resp.setContentType("application/json");
			int indent = ss == null ? 0 : Integer.parseInt(ss);
			r.write(resp.getWriter(), indent, true);
			resp.getWriter().flush();
		} catch (ExecutionException|IOException e) {
			reportError(500, e.getLocalizedMessage(), resp);
			logger.error("request failed:", e);
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