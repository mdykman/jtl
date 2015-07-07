package org.dykman.jtl.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListeningExecutorService;

public class CacheModule implements Module {

	JSONObject config;
	public CacheModule(JSONObject config) {
		this.config = config;
	}

	static class CacheHolder {
		JSONBuilder builder;
		Map<String,Cache<JSON, JSON>> caches = new HashMap<>();
		
		public CacheHolder(JSONBuilder builder) {
			this.builder = builder;
		}
		
		InstructionFuture<JSON> fetch() {
			return null;
		}
		
		public JSON get(String label,JSON param,InstructionFuture<JSON> inst) {
			try {
			Cache<JSON, JSON> cache  = caches.get(label);
			Callable<JSON> callable = new Callable<JSON>() {
				
				@Override
				public JSON call() throws Exception {
					
					// TODO Auto-generated method stub
					return null;
				}
			};
			
			return cache.get(param,callable);
			} catch(ExecutionException e) {
				return builder.value();
			}
		}
	}
	@Override
	public void define(AsyncExecutionContext<JSON> context) {
		CacheHolder ch= new CacheHolder(context.builder());
		context.define("fetch", ch.fetch());
	}

}
