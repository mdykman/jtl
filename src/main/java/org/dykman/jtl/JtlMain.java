package org.dykman.jtl;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.server.JtlServer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class JtlMain {

   JSONBuilder builder = new JSONBuilderImpl();
   InstructionFutureFactory factory = new InstructionFutureFactory();
   JtlCompiler compiler = new JtlCompiler(builder, false, false, false);
   ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

   JSON config = builder.value();

   public static void printHelp() {
      System.out.println("help!");
   }

   public static void main(String[] args) {
      // TODO Auto-generated method stub
      try {
         String s = System.getProperty("jtl.home");
         File home;
         if(s == null) {
            home = new File(".");
         } else {
            home = new File(s);
         }

         LongOpt[] opts = new LongOpt[8];
         {
            StringBuffer sb = new StringBuffer();
            opts[0] = new LongOpt("config", LongOpt.REQUIRED_ARGUMENT, sb, 'c');
            opts[1] = new LongOpt("jtl", LongOpt.REQUIRED_ARGUMENT, sb, 'x');
            opts[2] = new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, sb, 'p');
            opts[3] = new LongOpt("data", LongOpt.REQUIRED_ARGUMENT, sb, 'd');
            opts[4] = new LongOpt("help", LongOpt.REQUIRED_ARGUMENT, sb, 'h');
            opts[5] = new LongOpt("directory", LongOpt.REQUIRED_ARGUMENT, sb, 'D');
            opts[6] = new LongOpt("server", LongOpt.NO_ARGUMENT, sb, 's');
            opts[7] = new LongOpt("indent", LongOpt.REQUIRED_ARGUMENT, sb, 'i');
         }
         int c;
         boolean help = false;
         File fconfig = null;
         File jtl = null;
         File fdata = null;
         int indent = 3;
         File cwd = new File(".");
         boolean serverMode = false;
         int port = 7719; // default port

         Getopt g = new Getopt("jtl", args, "c:x:p:d:h:D:i:s", opts);
         while((c = g.getopt()) != -1) {
            switch(c) {
            case 0: {
               fconfig = new File(cwd, g.getOptarg());
            }
               break;
            case 1: {
               jtl = new File(cwd, g.getOptarg());
            }
               break;
            case 2: {
               port = Integer.parseInt(g.getOptarg());
               serverMode = true;
            }
               break;
            case 3: {
               fdata = new File(home, g.getOptarg());
            }
               break;
            case 4: {
               help = true;
            }
               break;
            case 5: {
               cwd = new File(home, g.getOptarg());
            }
               break;
            case 6: {
               serverMode = true;
            }
               break;
            case 7: {
               indent = Integer.parseInt(g.getOptarg());

            }
               break;
            }

         }

         if(help) {
            printHelp();
            System.exit(0);
         }

         JtlMain main = new JtlMain();
         if(fconfig != null)
            main.setConfig(fconfig);

         if(serverMode) {
            JtlServer server = main.launchServer(jtl, port);
            server.start();
            server.join();
         } else {
            if(jtl == null) {
               // if not specified with a switch, the script must be the first
               // name on the argument list
               int n = g.getOptind();
               String script = n >= args.length ? null : args[n];
               if(script == null) {
                  throw new RuntimeException("could not determine input script");
               }
               jtl = new File(script);
            }
            InstructionFuture<JSON> inst = main.compile(jtl);
            PrintWriter pw = new PrintWriter(System.out);
            if(fdata != null) {
               JSON data = main.parse(fdata);
               JSON result = main.execute(inst, data, cwd, fconfig);
               result.write(pw, indent, false);
               pw.flush();

            } else {
               for(int i = g.getOptind(); i < args.length; i++) {
                  File f = new File(args[i]);
                  JSON data = main.parse(f);
                  JSON result = main.execute(inst, data, cwd, fconfig);
                  result.write(pw, indent, false);
                  pw.flush();
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public void setConfig(File f) throws IOException {
      config = builder.parse(f);
   }

   public JSONObject getConfig(File f) throws IOException {
      JSON c;
      if(f != null && f.exists()) {
         c = builder.parse(f);
      } else {
         c = builder.value();
      }
      return builder.object(null);
   }

   public JtlServer launchServer(File base, int port) {
      JtlServer server = new JtlServer(base, config, les, port);
      return server;
   }

   public void shutdown() throws InterruptedException {
      les.shutdownNow();
      les.awaitTermination(2, TimeUnit.SECONDS);
   }

   public InstructionFuture<JSON> compile(File f) throws IOException {
      return compiler.parse(f);
   }

   public InstructionFuture<JSON> compile(String f) throws IOException {
      return compiler.parse(f);
   }

   public JSON parse(File f) throws IOException {
      return builder.parse(f);
   }

   public JSON execute(InstructionFuture<JSON> inst, JSON data, File cwd, File config) throws Exception {
      // JSON d = builder.parse(data);
      JSON c;
      if(config != null && config.exists()) {
         c = builder.parse(config);
      } else {
         c = builder.value();
      }
      AsyncExecutionContext<JSON> context = JtlCompiler.createInitialContext(data, c, cwd, builder, les);
      // InstructionFuture<JSON> inst = compiler.parse(jtl);
      ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
      return j.get();
   }
}
