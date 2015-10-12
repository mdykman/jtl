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

      AsyncExecutionContext<JSON> initContext = null;
      FutureInstruction<JSON> initInst = null;
      ListenableFuture<JSON> initResult = null;

      public ContextObjectInstructionFuture(SourceInfo meta,
      // InstructionFutureFactory factory,
            final List<Pair<String, FutureInstruction<JSON>>> ll) {
         // this.factory = factory;
         super(meta, ll, true);
         meta.name = "contextobject";
      }

      protected ListenableFuture<JSON> initializeContext(AsyncExecutionContext<JSON> ctx, FutureInstruction<JSON> inst,
            ListenableFuture<JSON> data) throws ExecutionException {
         if(initContext == null) {
            synchronized(this) {
               if(initContext == null) {
                  initResult = inst.call(ctx, data);
                  initContext = ctx;
               }
            }
         }
         ctx.inject(initContext);
         for(Map.Entry<String, AsyncExecutionContext<JSON>> nc : initContext.getNamedContexts().entrySet()) {
            ctx.inject(nc.getKey(), nc.getValue());
         }
         return initResult;
      }

      protected ListenableFuture<JSON> contextObject(final AsyncExecutionContext<JSON> ctx,
            final ListenableFuture<JSON> data, boolean imported) throws ExecutionException {
         FutureInstruction<JSON> defaultInstruction = null;
         FutureInstruction<JSON> startInstruction = null;
         FutureInstruction<JSON> init = null;
         List<FutureInstruction<JSON>> imperitives = new ArrayList<>(ll.size());
         // final AsyncExecutionContext<JSON> context = imported ?
         // ctx.getMasterContext() : ctx.createChild(false, false,data,
         // source);
         final AsyncExecutionContext<JSON> context = ctx;

         String m = ctx.method();
         String entryPoint = m == null ? "_" : "_" + m;

         for(Pair<String, FutureInstruction<JSON>> ii : ll) {
            final String k = ii.f;
            final FutureInstruction<JSON> inst = ii.s;

            if(k.equals("!init")) {
               if(initInst == null)
                  synchronized(this) {
                     if(initInst == null) {
                        initInst = init = fixContextData(memo(inst.getSourceInfo(), inst));
                     }
                  }
               context.define("init", initInst);
            } else if(k.equals("_")) {
               defaultInstruction = fixContextData( inst);
            } else if(k.equals(entryPoint)) {
               startInstruction = fixContextData(inst);
            } else if(k.startsWith("!")) {
               // variable, (almost) immediate evaluation
//                InstructionFuture<JSON> imp = inst;
                FutureInstruction<JSON> imp = fixContextData(inst);
               context.define(k.substring(1), imp);
               imperitives.add(imp);
            } else if(k.startsWith("$")) {
               // variable, deferred evaluation
               context.define(k.substring(1), fixContextData(FutureInstructionFactory.deferred(inst.getSourceInfo(), inst, context, data)));
            } else {
               // define a function
               context.define(k,fixContextData(inst));
            }
         }
         try {
            // ensure that init is completed so that any modules are
            // installed
            // and imports imported
            final FutureInstruction<JSON> finst = startInstruction == null ? defaultInstruction : startInstruction;
            AsyncFunction<List<JSON>, JSON> runner = new AsyncFunction<List<JSON>, JSON>() {

               @Override
               public ListenableFuture<JSON> apply(final List<JSON> input) throws Exception {
                  return finst != null && imported == false ? finst.call(context, data)
                        : immediateCheckedFuture(context.builder().value(true));
               }
            };

            if(initInst != null) {
               AsyncFunction<JSON, JSON> ff = new AsyncFunction<JSON, JSON>() {
                  @Override
                  public ListenableFuture<JSON> apply(final JSON input2) throws Exception {
                	  // let the memo do it's job
  //                   context.define("init", InstructionFutureFactory.value(input2, source));
                     List<ListenableFuture<JSON>> ll = new ArrayList<>();
                     for(FutureInstruction<JSON> imp : imperitives) {
                        ll.add(imp.call(context, data));
                     }
                     if(!ll.isEmpty())
                        return transform(allAsList(ll), runner);
                     if(finst != null)
                        return finst.call(context, data);
                     return immediateCheckedFuture(context.builder().value(true));
                  }
               };
               return transform(initializeContext(context.getInit(), initInst, context.config()), ff);
            }

            List<ListenableFuture<JSON>> ll = new ArrayList<>();
            for(FutureInstruction<JSON> imp : imperitives) {
               ll.add(imp.call(context, data));
            }
            if(!ll.isEmpty())
               return transform(allAsList(ll), runner);
            if(finst != null)
               return finst.call(context, data);
            return immediateCheckedFuture(context.builder().value(true));
         } catch (ExecutionException e) {
            FutureInstruction<JSON> error = context.getdef("error");
            if(error == null) {
               System.err.println("WTF!!!!???? no error handler is defined!");
               throw new RuntimeException("WTF!!!!???? no error handler is defined!");
            }
            AsyncExecutionContext<JSON> ec = context.createChild(true, false, data, source);
            ec.define("0", FutureInstructionFactory.value("error", context.builder(), source));
            ec.define("1", FutureInstructionFactory.value(500L, context.builder(), source));
            ec.define("2", FutureInstructionFactory.value(e.getLocalizedMessage(), context.builder(), source));
            return error.call(ec, data);
         }
      }

      @Override
      public ListenableFuture<JSON> _callObject(final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException {
         final AsyncExecutionContext<JSON> ctx = context.createChild(false, false, data, source);
 //        ctx.define("_", InstructionFutureFactory.value(data, source));
         return contextObject(ctx, data, context.isInclude());

      }

   }