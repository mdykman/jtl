package org.dykman.jtl.future;

import java.io.File;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class DeferredCall implements InstructionFuture<JSON> {
   final SourceInfo info;
	public final InstructionFuture<JSON> inst;
	public final AsyncExecutionContext<JSON> pcontext;
	public final ListenableFuture<JSON> data;

	public DeferredCall(SourceInfo source,InstructionFuture<JSON> inst,
			AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> t) {
	   this.inst = inst;
		this.pcontext = context;
		this.data = t;
		this.info = source;
	}
	
	public SourceInfo getSourceInfo() {
	   return info;
	}
//   @Override
	public AsyncExecutionContext<JSON> getContext() {
	   return pcontext;
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
            return getNamedContext(label,false,null);
         }

         @Override
         public AsyncExecutionContext<JSON> getNamedContext(
               String label, boolean create,SourceInfo info) {
            AsyncExecutionContext<JSON> c = pcontext.getNamedContext(label,create,info);
            
            /// NB: it is had to imagine this call succeeding if the previous one has not
            if(c == null) c = context.getNamedContext(label,create,info);
            return c;
         }

         @Override
         public AsyncExecutionContext<JSON> createChild(boolean fc,ListenableFuture<JSON> data,SourceInfo info) {
            return pcontext.createChild(fc,data,info);
         }

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
      }; 
	   return new DeferredCall(info,inst.unwrap(ctx), ctx, null);
	}

	@Override
	public ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException {
	   ListenableFuture<JSON> d = this.data != null ? this.data : data;
		return inst.call(this.pcontext, d);
	}
}
