package org.dykman.jtl.future;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

// rank: all
    class ObjectInstructionFuture extends ObjectInstructionBase {

      public ObjectInstructionFuture(SourceInfo meta, final List<Pair<String, FutureInstruction<JSON>>> ll) {
         super(meta, ll, true);
         meta.name = "dataobject";
      }

      protected ListenableFuture<JSON> dataObject(final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException {

         final List<ListenableFuture<Pair<String, JSON>>> insts = new ArrayList<>(ll.size());
         for(Pair<String, FutureInstruction<JSON>> ii : ll) {
            final String kk = ii.f;
            final AsyncExecutionContext<JSON> newc = context.createChild(false, false, data, source);
            FutureInstruction<JSON> ki = InstructionFutureFactory.value(kk, context.builder(), getSourceInfo());
  //          newc.define(InstructionFutureFactory.JTL_INTERNAL_KEY, ki);
//            newc.define("key", ki);
            newc.define(":", ki);
            ListenableFuture<Pair<String, JSON>> lf = transform(ii.s.call(newc, data),
                  new AsyncFunction<JSON, Pair<String, JSON>>() {
                     @Override
                     public ListenableFuture<Pair<String, JSON>> apply(JSON input) throws Exception {
                        input.setName(kk);
                        return immediateCheckedFuture(new Pair<>(kk, input));
                     }
                  });
            insts.add(lf);
         }

         return transform(allAsList(insts), new AsyncFunction<List<Pair<String, JSON>>, JSON>() {
            @Override
            public ListenableFuture<JSON> apply(List<Pair<String, JSON>> input) throws Exception {
               JSONObject obj = context.builder().object(null, input.size());

               for(Pair<String, JSON> d : input) {
                  obj.put(d.f, d.s != null ? d.s : context.builder().value());
               }
               return immediateCheckedFuture(obj);
            }
         });
      }

      @Override
      public ListenableFuture<JSON> _callObject(final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException {
         return dataObject(context, data);
      }

   }