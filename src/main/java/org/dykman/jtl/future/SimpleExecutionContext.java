package org.dykman.jtl.future;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.operator.FutureInstruction;
import org.dykman.jtl.operator.AbstractFutureInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class SimpleExecutionContext implements AsyncExecutionContext<JSON> {

	static Logger logger = LoggerFactory.getLogger(SimpleExecutionContext.class);
	
	protected final AsyncExecutionContext<JSON> parent;
	protected final boolean functionContext;
	protected final boolean include;
	protected boolean isInit = false;
	protected boolean isRuntime = false;
	protected Exception runtimeException;

	protected final Map<String, Object> things = new ConcurrentHashMap<>();

	protected String method = null;
	protected final Map<String, FutureInstruction<JSON>> functions;
	protected ListeningExecutorService executorService = null;
	protected JSON conf;
	protected ListenableFuture<JSON> data;
	protected AsyncExecutionContext<JSON> declarer;
	
	
	HttpServletRequest requestObject;
	HttpServletResponse responseObject;

	
	SourceInfo sourceInfo;

	File currentDirectory = new File(".");
	JSONBuilder builder = null;

	boolean debug = false;
	 JtlCompiler compiler = null;
	final Map<String, AsyncExecutionContext<JSON>> namedContexts = new ConcurrentHashMap<>();

	public boolean isFunctionContext() {
		return functionContext;
	}

	boolean isLocked = false;
	public void lock(boolean b) {
		isLocked = true;
	}

	public boolean isInclude() {
		return include;
	}
	public SourceInfo getSourceInfo() {
		return sourceInfo;
	}

	public Map<String, AsyncExecutionContext<JSON>> getNamedContexts() {
		return namedContexts;
	}

	@Override
	public Object get(String key) {
		return things.get(key);
	}

	@Override
	public void set(String key, Object o) {
		things.put(key, o);
	}

	public SimpleExecutionContext(AsyncExecutionContext<JSON> parent,Map<String, FutureInstruction<JSON>> func, JSONBuilder builder, ListenableFuture<JSON> data,
			JSON conf, File f,SourceInfo sourceInfo, boolean fc, boolean include, boolean debug) {
		this.parent = parent;
		this.functionContext = fc;
		this.builder = builder;
		this.conf = conf;
		this.data = data;
		this.currentDirectory = f;
		this.debug = debug;
		this.include = include;
		this.sourceInfo = sourceInfo;
		this.functions = func == null ?  new ConcurrentHashMap<>() : func;
		compiler = parent != null ? parent.compiler() : new JtlCompiler(builder);
	}

	public SimpleExecutionContext(JSONBuilder builder, Map<String, FutureInstruction<JSON>> func,ListenableFuture<JSON> data, JSON conf, File f,SourceInfo sourceInfo) {
		this(null, func,builder, data, conf, f,sourceInfo, false, false, false);
	}

	public void inject(AsyncExecutionContext<JSON> cc) {
		SimpleExecutionContext c = (SimpleExecutionContext) cc;
		functions.putAll(c.functions);
	}

	public void inject(String name, AsyncExecutionContext<JSON> cc) {
		SimpleExecutionContext c = (SimpleExecutionContext) cc;
		SimpleExecutionContext n = (SimpleExecutionContext) getNamedContext(name, true, false, null);
		n.functions.putAll(c.functions);
	}

	public String method() {
		return method;
	}

	public String method(String m) {
		return method = m;
	}

	public ListenableFuture<JSON> dataContext() {
		return data;
	}

	public ListenableFuture<JSON> config() {
		if (conf == null && parent != null)
			return parent.config();
		return immediateFuture(conf);
	}

	public JtlCompiler compiler(JtlCompiler c) {
		compiler = c;
		return c;
	}
	@Override
	public JtlCompiler compiler() {
		JtlCompiler c = compiler;
		AsyncExecutionContext<JSON> p = this.getParent();
		while (c == null && p != null) {
			c = p.compiler();
			p = p.getParent();
		}
		return c;
		
	}
	@Override
	public JSONBuilder builder() {
		JSONBuilder r = this.builder;
		AsyncExecutionContext<JSON> p = this.getParent();
		while (r == null && p != null) {
			r = p.builder();
			p = p.getParent();
		}
		return r;

	}

	Map<String, AtomicInteger> counterMap = Collections.synchronizedMap(new HashMap<>());

	@Override
	public synchronized int counter(String label, int interval) {
		AtomicInteger ai = counterMap.get(label);
		if (ai == null) {
			ai = new AtomicInteger();
			counterMap.put(label, ai);
		}
		return ai.addAndGet(interval);
	}

	@Override
	public boolean debug() {
		return debug;
	}

	@Override
	public boolean debug(boolean d) {
		return debug = d;
	}

	@Override
	public AsyncExecutionContext<JSON> getParent() {
		return parent;
	}

	/*
	 * @Override public AsyncExecutionContext<JSON> getMasterContext() {
	 * AsyncExecutionContext<JSON> c = this; AsyncExecutionContext<JSON> parent
	 * = c.getParent(); while (parent != null) { c = parent; parent =
	 * c.getParent(); } return c; }
	 */
	AsyncExecutionContext<JSON> initContext = null;
	AsyncExecutionContext<JSON> runtimeContext = null;

	@Override
	public AsyncExecutionContext<JSON> getInit() {
		if (isInit)
			return this;
		if (initContext != null)
			return initContext;
		if (parent != null) {
			initContext = parent.getInit();
		}
		return initContext;
	}

	@Override
	public AsyncExecutionContext<JSON> getRuntime() {
		if (isRuntime)
			return this;
		if (runtimeContext != null)
			return runtimeContext;
		if (parent != null) {
			runtimeContext = parent.getRuntime();
		}
		return runtimeContext;
	}

	@Override
	public AsyncExecutionContext<JSON> getFunctionContext() {
		if (functionContext)
			return this;
		if (parent != null) {
			return parent.getFunctionContext();
		}
		return null;
	}

	@Override
	public AsyncExecutionContext<JSON> getNamedContext(String label) {
		return getNamedContext(label, false, false, null);
	}

	public AsyncExecutionContext<JSON> getNamedContext(String label, boolean create, boolean include, SourceInfo info) {
		AsyncExecutionContext<JSON> c = namedContexts.get(label);
		if (c == null && create) {
			synchronized (this) {
				c = namedContexts.get(label);
				if (c == null) {
					if (create) {
						c = this.createChild(false, include, immediateFuture(JSONBuilderImpl.NULL), info);
						namedContexts.put(label, c);
					}
				}
			}
		}
		return c;
	}

	public void setParent(AsyncExecutionContext<JSON> c) {
		
	}

	@Override
	public void define(String n, FutureInstruction<JSON> i) {
		if(isLocked) throw new RuntimeException(String.format("tried to modify a locked context: %s",n));
		functions.put(n, i);
	}
	@Override
	public AsyncExecutionContext<JSON> createChild(boolean fc, boolean include, ListenableFuture<JSON> data,
			SourceInfo source) {
		return createChild(fc, include, null, data, source,false);
	}
	@Override
	public AsyncExecutionContext<JSON> createChild(boolean fc, boolean include, Map<String, FutureInstruction<JSON>> func,ListenableFuture<JSON> data,
			SourceInfo source,boolean uncouple) {
		AsyncExecutionContext<JSON> r = new SimpleExecutionContext(uncouple ? null : this, func,builder,
				data == null ? this.data : data, conf, currentDirectory(),source, fc,
				include, debug);
		
		r.setNamespace(getNamespace());
		return r;
	}

	protected String namespace = null;
	
	public Pair<String, FutureInstruction<JSON>> getNamespacedDefinition(String ns,String name) {
		AsyncExecutionContext<JSON> named = null;
		AsyncExecutionContext<JSON> base = this;
		while(base!=null && named == null) {
			named = base.getNamedContext(ns);
			if(named!=null) {
				FutureInstruction<JSON> fi = named.getdef(name);
				if(fi!=null) return new Pair<>(ns, fi);
				else named = null;
			}
			base = base.getParent();
		}
		return null;
	}
	@Override
	public Pair<String, FutureInstruction<JSON>> getDefPair(String name) {
		FutureInstruction<JSON> r = null;
		String fns = null;
		String[] parts = name.split("[.]", 2);
		if(parts.length == 2) {
			return getNamespacedDefinition(parts[0], parts[1]);
		} else {
			boolean special = isSpecial(name);
			AsyncExecutionContext<JSON> fc = this.getFunctionContext();
			String ns = fc!=null && !special ? fc.getNamespace() : null;
			if(ns!=null) {
				Pair<String, FutureInstruction<JSON>> pp = getNamespacedDefinition(ns, name);
				if(pp!=null) return pp;
			}
			SimpleExecutionContext sec = this;

			boolean isNumber = Character.isDigit(name.charAt(0));
			while(sec!=null && r==null) {
				r = sec.functions.get(name);
				if(isNumber && sec.isFunctionContext())  break;
				sec =(SimpleExecutionContext) sec.getParent();
			}
		}
		return new Pair<>(fns,r);
	}
	
	public FutureInstruction<JSON> getdef(String name) {
		if(name == null) {
			return new AbstractFutureInstruction(null) {
				public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data) {
					return immediateCheckedFuture(JSONBuilderImpl.NULL);
				}
			};
		}
		Pair<String,FutureInstruction<JSON>> pp = getDefPair(name);
		if(pp !=null) return pp.s;
		return null;
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
		return new File(currentDirectory, f);
	}

	@Override
	public AsyncExecutionContext<JSON> declaringContext() {
		AsyncExecutionContext<JSON> d = declarer;
		SimpleExecutionContext p = (SimpleExecutionContext) getParent();
		while (d == null && p != null) {
			d = p.declarer;
			p = (SimpleExecutionContext) p.getParent();
		}
		return d;
	}

	@Override
	public AsyncExecutionContext<JSON> declaringContext(AsyncExecutionContext<JSON> c) {
		// System.out.println("set declaring " + System.identityHashCode(c));
		return declarer = c;
	}

	@Override
	public void setInit(boolean b) {
		isInit = b;
	}

	@Override
	public boolean isInit() {
		return isInit;
	}

	@Override
	public void setRuntime(boolean b) {
		isRuntime = b;
	}

	@Override
	public boolean isRuntime() {
		return isRuntime;
	}

	List<ContextComplete> closers = new ArrayList<>();

	@Override
	public boolean cleanUp() {
		boolean result = true;
		for (ContextComplete f : closers) {
			result = result && f.complete();
		}
		return result;
	}

	public void onCleanUp(ContextComplete func) {
		closers.add(func);
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public Exception exception() {
		return runtimeException;
	}

	@Override
	public Exception exception(Exception e) {
		if(runtimeException==null) {
			runtimeException = e;
		}
		return runtimeException;
	}

	
	static Set<String> SpecialSymbols = new HashSet<>(Arrays.asList(
			new String[] {"$", "?", ":", ";", "#", "!", "%", "^", "&", "*" }));


	public static boolean isSpecial(String s) {
		return SpecialSymbols.contains(s)
			|| Character.isDigit(s.charAt(0));
	}


	@Override
	public HttpServletRequest getRequest() {
		HttpServletRequest r = requestObject;
		AsyncExecutionContext<JSON> p = this.getParent();
		while (r == null && p != null) {
			r = p.getRequest();
			p = p.getParent();
		}
		return r;
		
	}

	@Override
	public HttpServletResponse getResponse() {
		HttpServletResponse r = responseObject;
		AsyncExecutionContext<JSON> p = this.getParent();
		while (r == null && p != null) {
			r = p.getResponse();
			p = p.getParent();
		}
		return r;
		
	}

	@Override
	public void setRequest(HttpServletRequest request) {
		this.requestObject = request;
	}

	@Override
	public void setResponse(HttpServletResponse response) {
		this.responseObject = response;
	}

}
