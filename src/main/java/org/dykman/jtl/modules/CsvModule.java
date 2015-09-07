package org.dykman.jtl.modules;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AbstractInstructionFuture;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class CsvModule implements Module {

   JSONObject config;

   public CsvModule(JSONObject config) {
      this.config = config;
   }

   abstract class CsvInstructionFuture extends AbstractInstructionFuture {
      CsvInstructionFuture(SourceInfo cs) {
         super(cs);
      }

      protected CSVFormat fromConfig( JSONObject cc) throws ExecutionException {
         CSVFormat format = null;
         Charset cs = null;
         JSON j = config.get("charset");
         if(j != null) {
            String c = stringValue(j);
            if(Charset.isSupported(c)) {
               cs = Charset.forName(c);
            } else {
               throw new ExecutionException("CharSet " + c + " is not supported", source);
            }
         }
         j = config.get("format");
         if(j != null) {
            String c = stringValue(j);
            CSVFormat.valueOf(c);
            format = CSVFormat.valueOf(c);
            if(format == null) {
               throw new ExecutionException("unable to locate a CSVFormat from " + c, source);
            }
         }

         if(format == null) {
            j = config.get("sep");
            if(j != null) {
               String c = stringValue(j);
               format = CSVFormat.newFormat(c.charAt(0));
            }
         }
         if(format == null) {
            format = CSVFormat.DEFAULT;
         }
         return format;
      }
   }

   @Override
   public void define(SourceInfo meta, AsyncExecutionContext<JSON> context) {

      context.define("read", new AbstractInstructionFuture(meta) {

         @Override
         public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
               throws ExecutionException {

            InstructionFuture<JSON> fn = context.getdef("1");
            InstructionFuture<JSON> options = context.getdef("2");
            List<ListenableFuture<JSON>> ll = new ArrayList<>();
            ll.add(fn.call(context, data));
            if(options!=null) {
               ll.add(options.call(context, data));
            }
            return Futures.transform(Futures.allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {

               @Override
               public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
                  Iterator<JSON> jit = input.iterator();
                  Charset cs = Charset.forName("UTF-8");
                  CSVFormat format = null;
                  JSONBuilder builder = context.builder();
                  String file = stringValue(jit.next());
                  JSONObject conf = (JSONObject)(jit.hasNext() ? jit.next() : null);
                  JSONObject fconf = config;
                  if(conf!=null) {
                     fconf = fconf.overlay(conf);
                  }
                  JSON j = config.get("charset");
                  if(j!=null) {
                     cs = Charset.forName(stringValue(j));
                  }
                  j = config.get("format");
                  if(j!=null) {
                     format = CSVFormat.valueOf(stringValue(j));
                  }
                  j = config.get("seperator");
                  if(j!=null) {
                     format = CSVFormat.newFormat(stringValue(j).charAt(0));
                  }
                  j = config.get("header");
                  boolean headers = j == null ? false : j.isTrue();
                  CSVParser parser =  CSVParser.parse(new File(file), cs, format);
                  JSONArray arr = builder.array(null);
                  Map<String,Integer> headerMap = null;
                  String[] hm=null;
                  if(headers) {
                     headerMap = parser.getHeaderMap();
                     hm = new String[headerMap.size()];
                     for(Map.Entry<String, Integer> hh : headerMap.entrySet()) {
                        hm[hh.getValue()] = hh.getKey();
                     }
                  }
                  
                  for(CSVRecord rec: parser) {
                     if(hm!=null) {
                        int  i = 0;
                        JSONObject obj = builder.object(arr);
                        for(String s : hm) {
                           obj.put(s, builder.value(rec.get(i++)));
                        }
                        arr.add(obj);
                     } else {
                        JSONArray a2 = builder.array(arr);
                        rec.forEach(new Consumer<String>() {
                           @Override
                           public void accept(String t) {
                              a2.add(builder.value(t));
                           }
                        });
                        arr.add(a2);
                        
                     }
                  }
                  return Futures.immediateCheckedFuture(arr);
                  
               }
            });
         }
      });

      // TODO Auto-generated method stub

   }

}
