package org.dykman.jtl.modules;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleLoader {

   JSONBuilder builder;
   JSONObject baseConf;
   JSONObject modules;
   File base;
   
   static Logger logger = LoggerFactory.getLogger(ModuleLoader.class);

//   Set<Pair<String, String>> loaded = new HashSet<>();
//   Map<String, String> _loaded = new HashMap<>();

   public ModuleLoader(File base,JSONBuilder builder, JSONObject conf) 
   throws ExecutionException {
      this.builder = builder;
      this.baseConf = conf;
      this.base = base;
      modules = (JSONObject) baseConf.get("modules");
      if(modules == null) {
          logger.error("no modules are defined");
          throw new ExecutionException("no modules are defined",SourceInfo.internal("modules"));
       }
      if(logger.isInfoEnabled()) {
		  StringBuilder sb = new StringBuilder("available modules: ");
    	  for(Pair<String, JSON> pp: modules) {
    		  sb.append(pp.f).append(" ");
    	  }
    	  logger.info(sb.toString());
      }
   }

   private static ModuleLoader theInstance = null;

   public static ModuleLoader getInstance(File base,JSONBuilder builder, JSONObject config) 
   	throws ExecutionException {
      if(theInstance == null) {
         synchronized(ModuleLoader.class) {
            if(theInstance == null) {
               theInstance = new ModuleLoader(base,builder, config);
            }
         }
      }
      return theInstance;
   }

   protected String stringValue(JSON j) {
      if(j == null)
         return null;
      switch(j.getType()) {
      case STRING:
      case DOUBLE:
      case LONG:
         return ((JSONValue) j).stringValue();
      default:
         return null;
      }
   }

   public int create(
		   SourceInfo info, String name, String key, 
		   AsyncExecutionContext<JSON> context, boolean serverMode, JSONObject config) 
      throws ExecutionException {
      String klass = null;
      try {
         JSONObject mod = (JSONObject) modules.get(name);
         
         if(mod == null) {
        	 logger.error("module " + name + " is not defined");
            throw new ExecutionException("module " + name + " is not defined",info);
         }
         klass = stringValue(mod.get("class"));
                  Class<Module> kl = (Class<Module>) Class.forName(klass);
                  Constructor<Module> mc = kl.getConstructor(config.getClass());
                  Module o = mc.newInstance(config);
                  o.setKey(key);
                  o.define(info, context,serverMode);                  
 
        return 1;
      } catch (Exception e) {
         logger.error("error loading module " + name + " with class " + klass + ": " + e.getLocalizedMessage(),e);
         throw new ExecutionException("error loading module " + name, e,info);
      }

   }
}
