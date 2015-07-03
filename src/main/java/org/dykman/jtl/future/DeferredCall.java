package org.dykman.jtl.future;

import static com.google.common.util.concurrent.Futures.immediateFailedCheckedFuture;

import java.io.File;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DeferredCall implements InstructionFuture<JSON> {
	public final InstructionFuture<JSON> inst;
	public final AsyncExecutionContext<JSON> pcontext;
	public final ListenableFuture<JSON> data;

	public DeferredCall(final InstructionFuture<JSON> inst,
			final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> t) {
		this.inst = inst;
		this.pcontext = context;
		this.data = t;
	}

	@Override
	public InstructionFuture<JSON> unwrap(final AsyncExecutionContext<JSON> context) {
		//return inst.unwrap(context);
		
		return new AbstractInstructionFuture() {
			
			@Override
			public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> data) throws ExecutionException {
				AsyncExecutionContext<JSON> compound = new AsyncExecutionContext<JSON>() {

					@Override
					public void define(String n, InstructionFuture<JSON> i) {
						pcontext.define(n, i);
					}

					@Override
					public ListeningExecutorService executor() {
						return pcontext.executor();
					}

					@Override
					public JSONBuilder builder() {
						return pcontext.builder();
					}

					@Override
					public AsyncExecutionContext<JSON> getMasterContext() {
						return pcontext.getMasterContext();
					}

					@Override
					public ListenableFuture<JSON> config() {
						return pcontext.config();
					}

					@Override
					public ListenableFuture<JSON> dataContext() {
						return pcontext.dataContext();
					}

					@Override
					public AsyncExecutionContext<JSON> getParent() {
						return pcontext.getParent();
					}


					@Override
					public InstructionFuture<JSON> getdef(String name) {
						InstructionFuture<JSON> inst = pcontext.getdef(name);
						if(inst == null) {
							try {
								Integer.parseInt(name);
							} catch(NumberFormatException e) {
								// only look in local context if name is NOT numeric
								inst = context.getdef(name);
							}
						}
						return inst;
					}

	
					@Override
					public ListenableFuture<JSON> lookup(String name,
							ListenableFuture<JSON> t) throws ExecutionException {
						ListenableFuture<JSON> l = pcontext.lookup(name, data);
						if(l == null) 
							try {
								// only look in local context if name is NOT numeric
								Integer.parseInt(name);
							} catch(NumberFormatException e) {
								l = context.lookup(name, data);
							}
						return l;
					}

					@Override
					public AsyncExecutionContext<JSON> getNamedContext(
							String label) {
						return getNamedContext(label,false);
					}

					@Override
					public AsyncExecutionContext<JSON> getNamedContext(
							String label, boolean create) {
						AsyncExecutionContext<JSON> c = pcontext.getNamedContext(label,create);
						
						/// NB: it is had to imagine this call succeeding if the previous one has not
						if(c == null) c = context.getNamedContext(label,create);
						return c;
					}

					@Override
					public AsyncExecutionContext<JSON> createChild(boolean fc) {
						return pcontext.createChild(fc);
					}

					@Override
					public File currentDirectory() {
						return pcontext.currentDirectory();
					}

					@Override
					public File file(String in) {
						return pcontext.file(in);
					}
				};
				return inst.unwrap(compound).call(compound, data);
			}
		};
		
//		return inst.unwrap(context);
	}

	@Override
	public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException {
		return inst.call(this.pcontext, this.data);
	}
}
