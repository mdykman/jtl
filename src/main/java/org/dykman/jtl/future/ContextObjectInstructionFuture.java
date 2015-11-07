package org.dykman.jtl.future;

import static com.google.common.util.concurrent.Futures.allAsList;


import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import static org.dykman.jtl.future.FutureInstructionFactory.*;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

// rank: all
    class ContextObjectInstructionFuture extends ObjectInstructionBase {
      // final InstructionFutureFactory factory;


      public ContextObjectInstructionFuture(SourceInfo meta,
      // InstructionFutureFactory factory,
            final List<Pair<ObjectKey, FutureInstruction<JSON>>> ll) {
         // this.factory = factory;
         super(meta, ll, false);
         meta.name = "contextobject";
     	isContextObject = true;
      }

      protected ListenableFuture<JSON> contextObject(final AsyncExecutionContext<JSON> ctx,
            final ListenableFuture<JSON> data, List<Pair<ObjectKey, FutureInstruction<JSON>>> fields) 
            		throws ExecutionException {
         FutureInstruction<JSON> defaultInstruction = null;
         final AsyncExecutionContext<JSON> context = ctx;

         for(Pair<ObjectKey, FutureInstruction<JSON>> ii : fields) {
            final String k = ii.f.label;
            final boolean notQuoted = ! ii.f.quoted;
            final FutureInstruction<JSON> inst = ii.s;
            if(notQuoted && k.equals("_")) {
                defaultInstruction = fixContextData( inst);
            } else {
            	context.define(k,fixContextData(inst));
            }
         }
         return defaultInstruction.call(context, data);
      }

      @Override
      public ListenableFuture<JSON> _callObject(
//  			final FutureInstruction<JSON> init,
//  			final List<FutureInstruction<JSON>> imperitives, 
  			final List<Pair<ObjectKey, FutureInstruction<JSON>>> fields,
    		  final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException {
         final AsyncExecutionContext<JSON> ctx = context.createChild(false, false, data, source);
 //        ctx.define("_", InstructionFutureFactory.value(data, source));
         ListenableFuture<JSON> res = contextObject(ctx, data, fields);
         return res;
      }

   }