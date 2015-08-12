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
import org.dykman.jtl.future.*;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.DeferredCall;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class CacheModule implements Module {

   JSONObject config;

   public CacheModule(JSONObject config) {
      this.config = config;
   }
   @Override
   public void define(SourceInfo meta,AsyncExecutionContext<JSON> context) {
//      CacheHolder ch = new CacheHolder(context.builder());
      for(Pair<String, JSON> pp : config) {
         SourceInfo si = meta.clone();
         si.name="cache:"+pp.f;
         context.define(pp.f,new AbstractInstructionFuture(si,true) {
            final JSONObject c = (JSONObject) pp.s;
            Cache<JSON, JSON> cache = null;
            
            Cache<JSON, Boolean> misses = null;

            protected long getDef(Long l, long def) {
               if(l != null)return l;
               return def;
            }
            protected String getDef(String l, String def) {
               if(l != null)  return l;
               return def;
            }

            public Cache<JSON, Boolean> getMissCache() {
               if(misses == null)
                  synchronized(this) {
                     if(misses == null) {
                        JSON j = c.get("max");
                        final Long max = ((j == null) ? null : ((JSONValue) j).longValue());
                        
                        j = c.get("ttl");
                        final Long ttl = (j == null) ? null : ((JSONValue) j).longValue();
                        
                        j = c.get("on");
                        String on =  j == null ? null : ((JSONValue) j).stringValue();
                        on = getDef(on, "access");
                        
                        if("access".equals(on)) {
                           misses = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000)).initialCapacity(256)
                                 .expireAfterAccess(getDef(ttl, 30000), TimeUnit.MILLISECONDS)
                                 .build();
                        } else {
                           misses = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000)).initialCapacity(256)
                                 .expireAfterWrite(getDef(ttl, 30000), TimeUnit.MILLISECONDS)
                                 .build();
                        }
                     }
                  }
               return misses;
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
                        String on =  j == null ? null : ((JSONValue) j).stringValue();
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
//               final InstructionFuture<JSON> func = ff.unwrap(context);
 //              while(ff instanceof MemoInstructionFuture) {
                  ff = ff.unwrap(context);
//               }

               InstructionFuture<JSON> ffraw = ff;
               if(ff instanceof DeferredCall) {
                  String s = meta.code;
                  DeferredCall dc = (DeferredCall) ff;
                  AsyncExecutionContext<JSON> cc = dc.getContext();
                  List<ListenableFuture<JSON>> ll = new ArrayList<>();
 //                 ll.add(context.config());
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
                        String ss = s;
//                        JSON conf = jit.next();
                        JSONArray arr = context.builder().array(null);
                        while(jit.hasNext()) {
                           arr.add(jit.next());
                        } 
                        Cache<JSON, Boolean> mmc = getMissCache();
                        if(mmc.getIfPresent(arr) != null) {
                           return Futures.immediateCheckedFuture(context.builder().value());
                        } else {
                        JSON val = getCache().get(arr, new Callable<JSON>() {
                           @Override
                           public JSON call() throws Exception {
                              JSON v = ffraw.call(context, data).get();
                              if(v == null || v.getType() == JSONType.NULL) {
                                 mmc.put(arr, Boolean.TRUE);
                              }
                              return v;
                           }
                        });
                        
                        return Futures.immediateCheckedFuture(val);
                        }
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
