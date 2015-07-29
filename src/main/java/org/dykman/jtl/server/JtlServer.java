package org.dykman.jtl.server;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.Pair;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import static org.dykman.jtl.future.InstructionFutureFactory.*;

public class JtlServer {

   final Server server;
   final File base;
   JSONBuilder builder = new JSONBuilderImpl();

   File servletRoot = null;
   InstructionFuture<JSON> defaultProgram = null;

   ListeningExecutorService les;
   JSON config;
   int port;

   public JtlServer(File base, JSON config, ListeningExecutorService les, int port) {
      this.les = les;
      this.base = base;
      this.port = port;
      this.config = config;
      server = new Server(7719);
      ServletHandler handler = new ServletHandler();
      handler.addServletWithMapping(JtlServlet.class, "/*");
      server.setHandler(handler);
   }

   public void start() throws Exception {
      if(base.isDirectory()) {
         servletRoot = base;
      } else {
         servletRoot = base.getParentFile();
         JtlCompiler compiler = new JtlCompiler(builder);
         defaultProgram = compiler.parse(base);
      }

      server.start();
   }

   public void join() throws InterruptedException {
      server.join();
   }

   public class JtlServlet extends HttpServlet {

      @Override
      protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
         if(req.getMethod().equalsIgnoreCase("post")) {
            String ss = req.getParameter("indent");
            JSON data = builder.parse(req.getInputStream());
            AsyncExecutionContext<JSON> context = servletContext(req, data);
            try {
               if(defaultProgram != null) {
                  ListenableFuture<JSON> j = defaultProgram.call(context, Futures.immediateFuture(data));
                  int indent=ss == null ? 0 : Integer.parseInt(ss);
                  j.get().write(resp.getWriter(), indent, false);
                  resp.getWriter().flush();
               } else {
                  reportError(404,"dynamic loading not yet implemented",resp);
               }
            } catch (ExecutionException | InterruptedException | java.util.concurrent.ExecutionException e) {
               reportError(500,e.getLocalizedMessage(),resp);
            }

         }
      }
      
      protected AsyncExecutionContext<JSON> servletContext(HttpServletRequest req,JSON data) {
         AsyncExecutionContext<JSON> context = JtlCompiler.createInitialContext(data, config, servletRoot, builder,
               les);
         Pair<String,Integer> meta = new Pair<String, Integer>("http service", 0);
         String [] pp = req.getPathInfo().split("[/]");
         context.define("0", value(base.getPath(),builder,meta));
         for(int i = 1; i < pp.length; ++i) {
            context.define(Integer.toString(i), value(pp[i],builder,meta));
         }
         JSONObject object = builder.object(null);
         for(Map.Entry<String,String[]> el : req.getParameterMap().entrySet()) {
            JSONArray arr = builder.array(object);
            for(String sv: el.getValue()) {
               arr.add(builder.value(sv));
            }
            object.put(el.getKey(), arr);
         }
         context.define("params", value(object,meta));

         object= builder.object(null);
         Enumeration<String> hk = req.getHeaderNames();
         while(hk.hasMoreElements()) {
            String k = hk.nextElement();
            object.put(k, builder.value(req.getHeader(k)));
         }
         context.define("headers", value(object,meta));
         return context;
      }
      
      protected void reportError(int code, String message, HttpServletResponse resp) {
         resp.setStatus(code);
         try {
            resp.getWriter().println(message);
            resp.getWriter().flush();
         } catch (IOException e) {
            System.err.println("bailed during error: " + e.getLocalizedMessage());
         }
      }

   }
}
