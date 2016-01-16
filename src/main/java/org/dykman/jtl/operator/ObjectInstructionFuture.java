package org.dykman.jtl.operator;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

// rank: all
    public class ObjectInstructionFuture extends ObjectInstructionBase {

      public ObjectInstructionFuture(SourceInfo meta, final List<Pair<ObjectKey, FutureInstruction<JSON>>> ll) {
         super(meta, ll, true);
         meta.name = "dataobject";
      }

      protected ListenableFuture<JSON> dataObject(
    		  final List<Pair<ObjectKey, FutureInstruction<JSON>>> fields,
    		  final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException {

         final List<ListenableFuture<Pair<String, JSON>>> insts = new ArrayList<>(fields.size());
         for(Pair<ObjectKey, FutureInstruction<JSON>> ii : fields) {
            final String kk = ii.f.label;
            FutureInstruction<JSON> ki = null;
            if(kk == null) {
            	ki = FutureInstructionFactory.memo(ii.f.expr);
            } else {
            	ki = FutureInstructionFactory.value(kk, context.builder(), getSourceInfo());
            }
            context.define(":", ki);
            
            ListenableFuture<Pair<String, JSON>> lf = transform(
            		allAsList(ki.call(context, data), ii.s.call(context, data)),
                  new AsyncFunction<List<JSON>, Pair<String, JSON>>() {
                     @Override
                     public ListenableFuture<Pair<String, JSON>> apply(List<JSON> input) throws Exception {
                    	 JSON keydata = input.get(0);
                    	 JSON i = input.get(1);
                    	 String kk = keydata.stringValue();
                        i.setName(kk);
                        return immediateCheckedFuture(new Pair<>(kk, i));
                     }
                  });
            insts.add(lf);
         }

         return transform(allAsList(insts), new AsyncFunction<List<Pair<String, JSON>>, JSON>() {
            @Override
            public ListenableFuture<JSON> apply(List<Pair<String, JSON>> input) throws Exception {
               JSONObject obj = context.builder().object(null, input.size());

               for(Pair<String, JSON> d : input) {
                  obj.put(d.f, d.s != null ? d.s : JSONBuilderImpl.NULL);
               }
               return immediateCheckedFuture(obj);
            }
         });
      }

      @Override
      public ListenableFuture<JSON> _callObject(
    		  final List<Pair<ObjectKey, FutureInstruction<JSON>>> fields, 
    		  final AsyncExecutionContext<JSON> context,
            final ListenableFuture<JSON> data) throws ExecutionException {
         return dataObject(fields,context, data);
      }

   }