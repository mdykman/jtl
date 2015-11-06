package org.dykman.jtl;

import java.util.Formatter;
import java.util.Locale;

import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;

public class SourceInfo {

   static SourceInfo internal = new SourceInfo();
   static {
	   internal.name = "internal";
	   internal.line = internal.position = internal.endline = internal.endposition = 0;
	   internal.code = internal.source = "*internal*";
   }
   public String name;
   public int line;
   public int position;
   public int endline;
   public int endposition;
   public String source;
   public String code;

   
   public SourceInfo() {
   }
   
   public static SourceInfo internal(String name) {
	   SourceInfo si = internal.clone();
	   si.name = name;
	   return si;
   }
   public SourceInfo clone() {
      SourceInfo si = new SourceInfo();
      si.name = name;
      si.line = line;
      si.position = position;
      si.source = source;
      si.code = code;
      si.endline = endline;
      si.endposition = endposition;
      return si;
   }
   
   public JSONObject toJson(JSONBuilder builder) {
	   JSONObject obj = builder.object(null);
	   obj.put("name", builder.value(name));
	   obj.put("source", builder.value(source));
	   obj.put("code", builder.value(code));
	   
	   obj.put("line", builder.value(line));
	   obj.put("endline", builder.value(endline));
	   obj.put("position", builder.value(position));
	   obj.put("endposition", builder.value(endposition));

	   return obj;
   }

   // @Override
   public String toString(AsyncExecutionContext<JSON> context) {
      // System.err.print(System.identityHashCode(context));
      StringBuilder sb = new StringBuilder();
      Formatter formatter = new Formatter(sb, Locale.CANADA);
      formatter.format("%12h::", System.identityHashCode(context));
      if(context != null) {
         AsyncExecutionContext<JSON> master = context.getRuntime();
         if(master == null) master = context.getInit();
         int n = master.counter("trace", 1);
         for(int i = 0; i < n; ++i) {
            sb.append("  ");
         }
      }
      formatter.format("%8s %d:%d-%d:%d %s", name, line, position, endline, endposition, code);
      formatter.close();
      return sb.toString();
   }

}
