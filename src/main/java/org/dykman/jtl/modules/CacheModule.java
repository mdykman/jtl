package org.dykman.jtl.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.operator.AbstractFutureInstruction;
import org.dykman.jtl.operator.DeferredCall;
import org.dykman.jtl.operator.FutureInstruction;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class CacheModule extends AbstractModule {

	static final ListenableFuture<JSON> NO_RESULT = Futures.immediateCheckedFuture(JSONBuilderImpl.NULL);

	public CacheModule(String key, JSONObject config) {
		super(key,config);
	}
	static Logger logger = LoggerFactory.getLogger(CacheModule.class);

	@Override
	public JSON define(SourceInfo meta, AsyncExecutionContext<JSON> context,boolean serverMode) {
		for (Pair<String, JSON> pp : config) {
			if ("server-mode".equals(pp.f)) {
				// ignore this value..
//				serverMode = pp.s.isTrue();
			} else {
				SourceInfo si = meta.clone();
				si.name = bindingKey + ":" + pp.f;
				context.define(pp.f, new AbstractFutureInstruction(si) {
					final JSONObject cacheConfig = (JSONObject) pp.s;
					JSON mm = cacheConfig.get("misses");
					final boolean useMissCache = mm!=null && mm.isTrue();
					private Cache<JSON, ListenableFuture<JSON>> cache = null;

					private Cache<JSON, Boolean> misses = null;

					protected long getDef(Long l, long def) {
						if (l != null)
							return l;
						return def;
					}

					protected String getDef(String l, String def) {
						if (l != null)
							return l;
						return def;
					}

					public Cache<JSON, Boolean> getMissCache() {
						if(!useMissCache) return null;
						if (misses == null)
							synchronized (this) {
								if (misses == null) {
									JSON j = cacheConfig.get("max");
									final Long max = ((j == null) ? null : ((JSONValue) j).longValue());

									j = cacheConfig.get("missttl");
									final Long ttl = (j == null) ? null : ((JSONValue) j).longValue();

									j = cacheConfig.get("on");
									String on = j == null ? null : ((JSONValue) j).stringValue();
									on = getDef(on, "access");

									if(logger.isInfoEnabled()) {
										logger.info("creating negative cache " + si.name + " max="+max+",ttl="+ttl+",on="+on);
									}
									if ("access".equals(on)) {
										misses = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000))
												.initialCapacity(256)
												.expireAfterAccess(getDef(ttl, 30000), TimeUnit.MILLISECONDS).build();
									} else {
										misses = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000))
												.initialCapacity(256)
												.expireAfterWrite(getDef(ttl, 30000), TimeUnit.MILLISECONDS).build();
									}
								}
							}
						return misses;
					}

					public Cache<JSON, ListenableFuture<JSON>> getCache() {
						if (cache == null)
							synchronized (this) {
								if (cache == null) {
									JSON j = cacheConfig.get("max");
									final Long max = ((j == null) ? null : ((JSONValue) j).longValue());

									j = cacheConfig.get("ttl");
									final Long ttl = (j == null) ? null : ((JSONValue) j).longValue();

									j = cacheConfig.get("on");
									String on = j == null ? null : ((JSONValue) j).stringValue();
									on = getDef(on, "access");

									if(logger.isInfoEnabled()) {
										logger.info("creating cache " + si.name + " max="+max+",ttl="+ttl+",on="+on);
									}
									if ("access".equals(on)) {
										cache = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000))
												.initialCapacity(256)
												.expireAfterAccess(getDef(ttl, 30000), TimeUnit.MILLISECONDS).build();
									} else {
										cache = CacheBuilder.newBuilder().maximumSize(getDef(max, 10000))
												.initialCapacity(256)
												.expireAfterWrite(getDef(ttl, 30000), TimeUnit.MILLISECONDS).build();
									}
								}
							}
						return cache;
					}

					@Override
					public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context,
							ListenableFuture<JSON> data) throws org.dykman.jtl.ExecutionException {
						FutureInstruction<JSON> ff = context.getdef("1");

						FutureInstruction<JSON> ffraw = ff.unwrap();
						if (ffraw instanceof DeferredCall) {
							DeferredCall dc = (DeferredCall) ffraw;
							AsyncExecutionContext<JSON> cc = dc.getContext();
							List<ListenableFuture<JSON>> ll = new ArrayList<>();
							// ll.add(context.config());
							for (int i = 1;; ++i) {
								FutureInstruction<JSON> inst = cc.getdef(Long.toString(i));
								if (inst == null)
									break;
								ll.add(inst.call(context, data));
							}
							return Futures.transform(Futures.allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
									Iterator<JSON> jit = input.iterator();
									JSONArray arr = context.builder().array(null);
									while (jit.hasNext()) {
										arr.add(jit.next());
									}
									
									Cache<JSON, Boolean> mmc = getMissCache();
									if (mmc!=null && mmc.getIfPresent(arr) != null) {
										return NO_RESULT;
									} else {
										ListenableFuture<JSON> val = getCache().get(arr, new Callable<ListenableFuture<JSON>>() {
											@Override
											public ListenableFuture<JSON> call() throws Exception {
												ListenableFuture<JSON> v = ffraw.call(context, data);
												return Futures.transform(v, new AsyncFunction<JSON, JSON>() {

													@Override
													public ListenableFuture<JSON> apply(JSON input) throws Exception {
														if(input== null || input.getType() == JSONType.NULL) {
															mmc.put(arr, Boolean.TRUE);
															return NO_RESULT;
														}
														return Futures.immediateCheckedFuture(input);
													}
													
												});
											}
										});

										return val;
									}
								}
							});
						}
					
						throw new ExecutionException("cache parameter is not a deferred call!!",source);
					}
				});
			}
		}
		return context.builder().value(1);
	}

}
