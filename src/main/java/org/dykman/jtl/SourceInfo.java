package org.dykman.jtl;

import java.util.Formatter;
import java.util.Locale;

import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

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
