package org.dykman.jtl.future;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class MemoInstructionFuture extends AbstractInstructionFuture {
   private ListenableFuture<JSON> result = null;
   private boolean fired = false;
   InstructionFuture<JSON> inst;
   
   MemoInstructionFuture(SourceInfo info,InstructionFuture<JSON> inst) {
      super(info);
      this.inst = inst;
   }
   @Override
   public InstructionFuture<JSON> unwrap(AsyncExecutionContext<JSON> context) {
      return inst.unwrap(context);
   }

   @Override
   public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
         throws ExecutionException {
      if(!fired) {
         synchronized(this) {
            if(!fired) {
               result = inst.call(context, data);
               fired = true;
            }
         }
      }
      return result;
   }
}