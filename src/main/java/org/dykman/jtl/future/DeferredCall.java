package org.dykman.jtl.future;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DeferredCall implements InstructionFuture<JSON> {
   final SourceInfo info;
	public final InstructionFuture<JSON> inst;
	public AsyncExecutionContext<JSON> pcontext;
	public final ListenableFuture<JSON> data;

	public DeferredCall(SourceInfo source,InstructionFuture<JSON> inst,
			AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> t) {
	   this.inst = inst;
		this.pcontext = context;
		this.data = t;
		this.info = source;
	}



	@Override
	public InstructionFuture<JSON> getBareInstruction() {
	   return inst.getBareInstruction();
	}

	public SourceInfo getSourceInfo() {
	   return info;
	}
//   @Override
	public AsyncExecutionContext<JSON> getContext() {
	   return pcontext;
	}
	
	  public DeferredCall rebindContext(final AsyncExecutionContext<JSON> context) {
	     return new DeferredCall(info, inst, context, data);
	  }
	@Override
	public InstructionFuture<JSON> unwrap(final AsyncExecutionContext<JSON> context) {
	   AsyncExecutionContext<JSON> ctx = new AsyncExecutionContext<JSON>() {

         @Override
         public void define(String n, InstructionFuture<JSON> i) {
            pcontext.define(n, i);
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
         public AsyncExecutionContext<JSON> getNamedContext(
               String label) {
            return getNamedContext(label);
         }

         @Override
         public AsyncExecutionContext<JSON> getNamedContext(
               String label, boolean create,boolean imp,SourceInfo info) {
            AsyncExecutionContext<JSON> c = pcontext.getNamedContext(label,create,imp,info);
            
            /// NB: it is had to imagine this call succeeding if the previous one has not
            if(c == null) c = context.getNamedContext(label,create,imp,info);
            return c;
         }
/*
         @Override
         public AsyncExecutionContext<JSON> createChild(boolean fc,boolean imp,ListenableFuture<JSON> data,SourceInfo info) {
            return pcontext.createChild(fc,imp,data,info);
         }
*/
         @Override
         public File currentDirectory() {
            return pcontext.currentDirectory();
         }

         @Override
         public File file(String in) {
            return pcontext.file(in);
         }

         @Override
         public boolean debug() {
            return pcontext.debug();
         }

         @Override
         public boolean debug(boolean d) {
            return pcontext.debug(d);
         }

         @Override
         public int counter(String label,int increment) {
            return pcontext.counter(label,increment);
         }

         @Override
         public String method() {
            return pcontext.method();
         }

         @Override
         public String method(String m) {
            return pcontext.method(m);
         }

         @Override
         public void inject(AsyncExecutionContext<JSON> cc) {
            pcontext.inject(cc);
            
         }

         @Override
         public void inject(String name, AsyncExecutionContext<JSON> cc) {
            pcontext.inject(name,cc);
         }

         @Override
         public Map<String, AsyncExecutionContext<JSON>> getNamedContexts() {
            return pcontext.getNamedContexts();
         }

         @Override
         public AsyncExecutionContext<JSON> declaringContext() {
            return pcontext.declaringContext();
         }

         @Override
         public AsyncExecutionContext<JSON> declaringContext(AsyncExecutionContext<JSON> c) {
            return pcontext.declaringContext(c);
         }

         @Override
         public boolean isFunctionContext() {
            return pcontext.isFunctionContext();
         }

         @Override
         public boolean isInclude() {
            return pcontext.isInclude();
         }

         @Override
         public AsyncExecutionContext<JSON> createChild(boolean fc, boolean include,
               ListenableFuture<JSON> dataContext, SourceInfo source) {
            return pcontext.createChild(fc, include, dataContext, source);
         }
      }; 
      return new DeferredCall(info,inst.unwrap(ctx), ctx, null);
//      return new DeferredCall(info,inst.unwrap(ctx), ctx, null);
	}

	@Override
	public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException {
	   ListenableFuture<JSON> d = this.data != null ? this.data : data;
		return inst.call(this.pcontext, d);
	}
}
