package org.dykman.jtl.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.antlr.v4.codegen.model.ListenerDispatchMethod;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;

import com.google.common.util.concurrent.ListeningExecutorService;

public class JtlServer {

	final Server server;
	final File serverBase;
//	final File script;
//	JSONBuilder builder = new JSONBuilderImpl();

//	File servletRoot = null;
	File init = null;
	InstructionFuture<JSON> defaultProgram = null;

//	ListeningExecutorService les;
//	JSON config;
//	int port;


	public JtlServer(File jtlBase,File serverBase, File init, File script, File config, String bindAddress, int port,boolean canonical) 
			throws IOException {
		this.serverBase = serverBase;
		InetSocketAddress binding = bindAddress == null ?
				new InetSocketAddress("127.0.0.1", port)
				: new InetSocketAddress(bindAddress, port);
				
		server = new Server(binding);
		server.addLifeCycleListener(new Listener() {

			@Override
			public void lifeCycleStarting(LifeCycle event) {
				System.err.println("starting server");
			}

			@Override
			public void lifeCycleStarted(LifeCycle event) {
				System.err.println("server started at port " + port);
			}

			@Override
			public void lifeCycleFailure(LifeCycle event, Throwable cause) {
			}

			@Override
			public void lifeCycleStopping(LifeCycle event) {
			}

			@Override
			public void lifeCycleStopped(LifeCycle event) {
			}
		});

		ServletHandler handler = new ServletHandler();
		ServletHolder holder = new ServletHolder(new JtlServlet());
		
		holder.setInitParameter("jtlbase", jtlBase.getCanonicalPath());
		holder.setInitParameter("root", serverBase.getCanonicalPath());
		if(config!=null) holder.setInitParameter("config", config.getCanonicalPath());
		if(script!=null) holder.setInitParameter("script", script.getCanonicalPath());
		if(init!=null) holder.setInitParameter("init", init.getCanonicalPath());
		holder.setInitParameter("canonical", canonical ? "true" : "false");
		handler.addServletWithMapping(holder, "/*");
		server.setHandler(handler);
	}

	public void start() throws Exception {
		server.start();
	}

	public void join() throws InterruptedException {
		server.join();
	}
}
