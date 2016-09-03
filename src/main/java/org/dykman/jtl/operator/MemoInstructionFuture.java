package org.dykman.jtl.operator;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class MemoInstructionFuture extends AbstractFutureInstruction {
   final FutureInstruction<JSON> inst;
   final String key;
   
   public MemoInstructionFuture(FutureInstruction<JSON> inst) {
      super(inst.getSourceInfo());
      this.inst = inst;
      // TODO a stronger key would be advisable
      this.key = "@memo-"+Long.toHexString(System.identityHashCode(this));
   }
   @Override
   public FutureInstruction<JSON> unwrap() {
      return inst.unwrap();
   }

   @Override
   public FutureInstruction<JSON> getBareInstruction() {
      return inst.getBareInstruction();
   }

   @Override
   public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
         throws ExecutionException {
      AsyncExecutionContext<JSON> pp = context.getRuntime();
      if(pp == null) pp = context.getInit();
    		  //context.getMasterContext();
      FutureInstruction<JSON> ic = pp.getdef(key);
      if(ic==null) {
         synchronized(this) {
            ic = pp.getdef(key);
            if(ic==null) {
               ListenableFuture<JSON>  r = inst.call(context, data);
               pp.define(key, FutureInstructionFactory.value(r, source));
               return r;
            }
         }
      }
      return ic.call(context, data); 
   }
}