package org.dykman.jtl.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AbstractInstructionFuture;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.DeferredCall;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class CacheModule implements Module {

   JSONObject config;

   public CacheModule(JSONObject config) {
      this.config = config;
   }

   static class CacheHolder {
      JSONBuilder builder;
      Map<String, Cache<JSON, JSON>> caches = new HashMap<>();

      public CacheHolder(JSONBuilder builder) {
         this.builder = builder;
      }

      InstructionFuture<JSON> fetch() {
         return null;
      }

      public JSON get(String label, JSON param, InstructionFuture<JSON> inst) {
         try {
            Cache<JSON, JSON> cache = caches.get(label);
            Callable<JSON> callable = new Callable<JSON>() {

               @Override
               public JSON call() throws Exception {

                  // TODO Auto-generated method stub
                  return null;
               }
            };

            return cache.get(param, callable);
         } catch (ExecutionException e) {
            return builder.value();
         }
      }
   }

   @Override
   public void define(SourceInfo meta,AsyncExecutionContext<JSON> context) {
      CacheHolder ch = new CacheHolder(context.builder());
      for(Pair<String, JSON> pp : config) {
         SourceInfo si = meta.clone();
         si.name="cache:"+pp.f;
         context.define(pp.f, new AbstractInstructionFuture(si) {
            final JSONObject c = (JSONObject) pp.s;


            Cache<JSON, JSON> cache = null;

            protected long getDef(Long l, long def) {
               if(l != null)
                  return l;
               return def;
            }

            protected String getDef(String l, String def) {
               if(l != null)
                  return l;
               return def;
            }

            public Cache<JSON, JSON> getCache() {
               if(cache == null)
                  synchronized(this) {
                     if(cache == null) {
                        JSON j = c.get("max");
                        final Long max = ((j == null) ? null : ((JSONValue) j).longValue());
                        
                        j = c.get("ttl");
                        final Long ttl = (j == null) ? null : ((JSONValue) j).longValue();
                        
                        j = c.get("on");
                        String on = ((JSONValue) j).stringValue();
                        on = getDef(on, "access");
                        if("access".equals(on)) {
                           cache = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000)).initialCapacity(256)
                                 .expireAfterAccess(getDef(ttl, 30000), TimeUnit.MILLISECONDS)
                                 .build();
                        } else {
                           cache = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000)).initialCapacity(256)
                                 .expireAfterWrite(getDef(ttl, 30000), TimeUnit.MILLISECONDS)
                                 .build();
                        }
                     }
                  }
               return cache;
            }

            @Override
            public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
                  throws org.dykman.jtl.ExecutionException {
               InstructionFuture<JSON> ff = context.getdef("1");
               final InstructionFuture<JSON> func = ff.unwrap(context);
               if(func instanceof DeferredCall) {
                  DeferredCall dc = (DeferredCall) func;
                  AsyncExecutionContext<JSON> cc = dc.getContext();
                  List<ListenableFuture<JSON>> ll = new ArrayList<>();
                  ll.add(context.config());
                  for(int i = 1;; ++i) {
                     InstructionFuture<JSON> inst = cc.getdef(Long.toString(i));
                     if(inst == null)
                        break;
                     ll.add(inst.call(context, data));
                  }
                  return Futures.transform(Futures.allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

                     @Override
                     public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
                        Iterator<JSON> jit = input.iterator();
                        JSON conf = jit.next();
                        JSONArray arr = context.builder().array(null);
                        while(jit.hasNext()) {
                           arr.add(jit.next());
                        }
                        return Futures.immediateCheckedFuture(getCache().get(arr, new Callable<JSON>() {
                           @Override
                           public JSON call() throws Exception {
                              return func.call(context, data).get();
                           }
                        }));
                     }
                  });
               }

               // TODO Auto-generated method stub
               return null;
            }
         });

      }
   }

}
