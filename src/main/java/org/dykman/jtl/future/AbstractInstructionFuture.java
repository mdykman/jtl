package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractInstructionFuture implements
		InstructionFuture<JSON> {
   protected SourceInfo source;
   public AbstractInstructionFuture(SourceInfo meta) {
      source=meta;
   }
   
   @Override
   public SourceInfo getSourceInfo() {
      return source;
   }

   @Override 
   public final ListenableFuture<JSON> call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
         throws ExecutionException {
      boolean debug = context.debug();
 //     AsyncExecutionContext<JSON> master = null;
      if(debug) {
         System.err.print(source.toString(context));
         System.err.println();
      }
      ListenableFuture<JSON> r =  _call(context,data);
      if(debug) {
         int n = context.getMasterContext().counter("trace",-1);
      }
      return r;
   }

   public abstract ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
         throws ExecutionException;

   protected int line;
   protected String code;
   
	@Override
	public InstructionFuture<JSON> unwrap(AsyncExecutionContext<JSON> context) {
		return this;
	}
/*
	protected Pair<String, Integer> getMeta() {
	   return new Pair<>(code,line);
	}
	*/
   protected static Long longValue(JSON j) {
      switch(j.getType()) {
      case LONG:
      case DOUBLE:
      case STRING:
         return ((JSONValue) j).longValue();
      default:
      }
      return null;
   }

   protected static Double doubleValue(JSON j) {
      switch(j.getType()) {
      case LONG:
      case DOUBLE:
      case STRING:
         return ((JSONValue) j).doubleValue();
      default:
      }
      return null;
   }

	protected static String stringValue(JSON j) {
		if (j == null)
			return null;
		switch (j.getType()) {
		case STRING:
		case DOUBLE:
		case LONG:
			return ((JSONValue) j).stringValue();
		default:
			return null;
		}
	}
/*
	public abstract ListenableFuture<JSON> call(
			AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException;
*/
}
