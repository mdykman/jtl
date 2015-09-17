package org.dykman.jtl.server;

import java.io.File;

import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import com.google.common.util.concurrent.ListeningExecutorService;

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
}
