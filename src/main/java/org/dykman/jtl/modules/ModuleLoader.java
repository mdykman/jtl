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

public class ModuleLoader {

   JSONBuilder builder;
   JSONObject modules;

//   Set<Pair<String, String>> loaded = new HashSet<>();
//   Map<String, String> _loaded = new HashMap<>();

   public ModuleLoader(JSONBuilder builder, JSONObject conf) {
      this.builder = builder;
      this.modules = conf;
      File mods = new File("modules.json");
      if(mods.exists())
         try {
            modules = (JSONObject) builder.parse(mods);
         } catch (IOException e) {
            throw new RuntimeException("while loading module config", e);
         }
   }

   private static ModuleLoader theInstance = null;

   public static ModuleLoader getInstance(JSONBuilder builder, JSONObject config) {
      if(theInstance == null) {
         synchronized(ModuleLoader.class) {
            if(theInstance == null) {
               theInstance = new ModuleLoader(builder, config);
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

   public int create(SourceInfo info, String name, AsyncExecutionContext<JSON> context, JSONObject config) 
      throws ExecutionException {
      String klass = null;
      try {
         if(modules == null) {
            System.err.println("no modules are defined");
            throw new RuntimeException("no modules are defined");
         }
         JSONObject mod = (JSONObject) modules.get(name);
         if(mod == null) {
            System.err.println("module " + name + " is not defined");
            throw new ExecutionException("module " + name + " is not defined",info);
         }
         klass = stringValue(mod.get("class"));
                  Class<Module> kl = (Class<Module>) Class.forName(klass);
                  Constructor<Module> mc = kl.getConstructor(config.getClass());
                  Module o = mc.newInstance(config);
                  o.define(info, context);                  
 
        return 1;
      } catch (Exception e) {
         System.err.println("error loading module " + name + " with class " + klass + ": " + e.getLocalizedMessage());
         throw new ExecutionException("error loading module " + name, e,info);
      }

   }
}
