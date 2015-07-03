package org.dykman.jtl.future;

import static com.google.common.util.concurrent.Futures.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class SimpleExecutionContext implements AsyncExecutionContext<JSON> {

	final AsyncExecutionContext<JSON> parent;
	final boolean functionContext;
	final Map<String, InstructionFuture<JSON>> functions = new ConcurrentHashMap<>();
	ListeningExecutorService executorService = null;
	JSON conf;
	JSON data;

	File currentDirectory = new File(".");
	JSONBuilder builder = null;
	final Map<String, AsyncExecutionContext<JSON>> namedContexts = new ConcurrentHashMap<>();

	public SimpleExecutionContext(AsyncExecutionContext<JSON> parent, JSONBuilder builder ,
		JSON data,JSON conf, File f, boolean fc) {
		this.parent = parent;
		this.functionContext = fc;
		this.builder = builder;
		this.conf = conf;
		this.data = data;
		this.currentDirectory = f;
	}
	public SimpleExecutionContext(JSONBuilder builder,JSON data,JSON conf,File f) {
		this(null,builder, data,conf, f,false);
	}


	public ListenableFuture<JSON> dataContext() {
		return immediateCheckedFuture(data);
	}
	public ListenableFuture<JSON> config() {
		if(conf==null && parent!=null) return parent.config();
		return immediateFuture(conf);
	}

	
	
	@Override
	public JSONBuilder builder() {
		JSONBuilder r = this.builder;
		AsyncExecutionContext<JSON> p = this.getParent();
		while(r  == null && p != null) {
			r = p.builder();
			p = p.getParent();
		}
		return r;
		
	}

	@Override
	public AsyncExecutionContext<JSON> getParent() {
		return parent;
	}
	@Override
	public AsyncExecutionContext<JSON> getMasterContext() {
		AsyncExecutionContext<JSON> c= this;
		AsyncExecutionContext<JSON> parent = c.getParent();
		while(parent!=null) {
			c= parent;
			parent = c.getParent();
		}
		return c;
	}
	@Override
	public AsyncExecutionContext<JSON> getNamedContext(String label) {
		return getNamedContext(label, true);
	}

	public AsyncExecutionContext<JSON> getNamedContext(String label,
			boolean create) {
		AsyncExecutionContext<JSON> c = namedContexts.get(label);
		if (c == null) {
			synchronized (this) {
				c = namedContexts.get(label);
				if (c == null) {
					if (parent != null) {
						c = parent.getNamedContext(label, false);
						if (c != null)
							return c;
					}
					if (create) {
						c = this.createChild(false);
						namedContexts.put(label, c);
					}
				}
			}
		}
		return c;
	}

	@Override
	public void define(String n, InstructionFuture<JSON> i) {
		functions.put(n, i);
	}


	@Override
	public AsyncExecutionContext<JSON> createChild(boolean fc) {
		return new SimpleExecutionContext(this,null,data,null,currentDirectory(), fc);
	}


	@Override
	public InstructionFuture<JSON> getdef(String name) {
		InstructionFuture<JSON> r = null;
		String[] parts = name.split("[.]");
		if (parts.length > 1) {
			AsyncExecutionContext<JSON> named = getNamedContext(parts[0]);
			int c = 1;
//			while (named != null && (c < (parts.length - 2))) {
//				named = named.getNamedContext(parts[c++]);
//			}
			if (named != null) {
				return named.getdef(parts[1]);
			}

		} else {
			r = functions.get(name);
			if (parent != null && r == null
					&& !(functionContext && Character.isDigit(name.charAt(0)))) {
				r = parent.getdef(name);

			}
		}
		return r;
	}

	@Override
	public ListenableFuture<JSON> lookup(String name, ListenableFuture<JSON> t)
			throws ExecutionException {
		InstructionFuture<JSON> inst = getdef(name);
		if (inst != null) {
			return inst.call(this, t);
		}
		// System.err.println("lookup failed: " + name);
		return immediateFailedCheckedFuture(new ExecutionException(
				"lookup failed: " + name));
	}

	public void setExecutionService(ListeningExecutorService s) {
		executorService = s;
	}


	@Override
	public ListeningExecutorService executor() {
		if (executorService != null)
			return executorService;
		if (parent != null)
			return parent.executor();
		return null;
	}

	@Override
	public File currentDirectory() {
		return currentDirectory;
	}
	public File file(String f) {
		return new File(currentDirectory,f);
	}

}
