package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractInstructionFuture implements
		InstructionFuture<JSON> {

   public AbstractInstructionFuture(Pair<String,Integer> meta) {
      this(meta.s,meta.f);
   }

   public AbstractInstructionFuture(int line,String code) {
      this.line = line;
      this.code = code;
   }
   protected int line;
   protected String code;
   
	@Override
	public InstructionFuture<JSON> unwrap(AsyncExecutionContext<JSON> context) {
		return this;
	}

	protected Pair<String, Integer> getMeta() {
	   return new Pair<>(code,line);
	}
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

	public abstract ListenableFuture<JSON> call(
			AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException;

}
