package org.dykman.jtl.future;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JList;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractInstructionFuture implements
		InstructionFuture<JSON> {
   final protected SourceInfo source;
   final protected boolean items;
   
   public AbstractInstructionFuture(SourceInfo meta) {
      this(meta == null ? SourceInfo.internal("default") : meta,false);
   }
   
   public AbstractInstructionFuture(SourceInfo meta,boolean i) {
      source=meta;
      items=i;
      
   }
   
   @Override
   public SourceInfo getSourceInfo() {
      return source;
   }

   public InstructionFuture<JSON> getBareInstruction() {
      return this;
   }
   
   protected void addToFrame(JList f, Iterable<JSON> ij, boolean recurse) {
      for(JSON j : ij) {
         if(j != null && j.getType() != JSONType.NULL)
            if(recurse && j.getType() == JSONType.LIST) {
               addToFrame(f, (JList) j, false);
            } else {
               f.add(j);
            }
      }
   }

   @Override 
   public final ListenableFuture<JSON> call(final AsyncExecutionContext<JSON> context,final ListenableFuture<JSON> data)
         throws ExecutionException {
      boolean debug = context.debug();
      if(debug) {
         System.err.print(source.toString(context));
         System.err.println();
      }
      
      ListenableFuture<JSON> r;
      if(!items) {
         r =  _call(context,data);
      } else {
         return Futures.transform(data, new AsyncFunction<JSON, JSON>() {
            @Override
            public ListenableFuture<JSON> apply(final JSON input) throws Exception {
               if(input.getType() == JSONType.LIST) {
                  List<ListenableFuture<JSON>> ll = new ArrayList<>();
                  for(JSON j : (JList) input) {
                     ll.add(_call(context, Futures.immediateCheckedFuture(j)));
                  }
                  return Futures.transform(Futures.allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

                     @Override
                     public ListenableFuture<JSON> apply(List<JSON> input2) throws Exception {
                        JList frame = context.builder().frame(input.getParent());
                        addToFrame(frame, input2, true);
                        return Futures.immediateCheckedFuture(frame);
                     }
                  });
               }
               return _call(context, Futures.immediateCheckedFuture(input));
            }
         });
      }      
      if(debug) {
    	  AsyncExecutionContext<JSON> cc = context.getInit();
         int n = cc.counter("trace",-1);
      }
      return r;
   }

   public abstract ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
         throws ExecutionException;

   protected int line;
   protected String code;
   
	@Override
	public InstructionFuture<JSON> unwrap() {
		return this;
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

}
