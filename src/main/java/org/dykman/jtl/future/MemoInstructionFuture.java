package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class MemoInstructionFuture extends AbstractInstructionFuture {
   final InstructionFuture<JSON> inst;
   final String key;
   
   MemoInstructionFuture(SourceInfo info,InstructionFuture<JSON> inst) {
      super(info);
      this.inst = inst;
      // TODO a stronger key would be advisable
      this.key = "@memo-"+Long.toHexString(System.identityHashCode(this));
   }
   @Override
   public InstructionFuture<JSON> unwrap(AsyncExecutionContext<JSON> context) {
      return inst.unwrap(context);
   }

   @Override
   public InstructionFuture<JSON> getBareInstruction() {
      return inst.getBareInstruction();
   }

   @Override
   public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
         throws ExecutionException {
      AsyncExecutionContext<JSON> pp = context.getMasterContext();
      InstructionFuture<JSON> ic = pp.getdef(key);
      if(ic==null) {
         synchronized(this) {
            ic = pp.getdef(key);
            if(ic==null) {
               ListenableFuture<JSON>  r = inst.call(context, data);
               pp.define(key, InstructionFutureFactory.value(r, source));
               return r;
            }
         }
      }
      return ic.call(context, data); 
   }
}