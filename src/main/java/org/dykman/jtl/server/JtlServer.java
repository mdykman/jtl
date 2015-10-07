package org.dykman.jtl.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;

public class JtlServer {

	final Server server;
	final File serverBase;

	File init = null;
	InstructionFuture<JSON> defaultProgram = null;

	public JtlServer(File jtlBase,File serverBase, File init, File script, File config, String bindAddress, int port,boolean canonical) 
			throws IOException {
		this.serverBase = serverBase;
		String host = bindAddress == null ? "127.0.0.1" : bindAddress;
		InetSocketAddress binding = new InetSocketAddress(host	, port);
				
		server = new Server(binding);
		server.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
			@Override
			public void lifeCycleStarting(LifeCycle event) {
				System.err.println("starting jtl server serving directory " + serverBase.getPath());
			}

			@Override
			public void lifeCycleStarted(LifeCycle event) {
				System.err.println("server started, listening on " + binding.toString());
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
