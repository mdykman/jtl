package org.dykman.jtl;

//import gnu.getopt.Getopt;
//import gnu.getopt.LongOpt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;
//import org.dykman.jtl.server.JtlServer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@SuppressWarnings("deprecation")
public class JtlMain {

   final JSONBuilder builder;
   InstructionFutureFactory factory = new InstructionFutureFactory();
   JtlCompiler compiler;
   ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

   JSON config;

   public JtlMain(boolean canonical) {
      builder = new JSONBuilderImpl(canonical);
      compiler = new JtlCompiler(builder, false, false, false);
      config = builder.value(); // initial config value
   }

   public static void printHelp(Options cl) {
      System.out.println(
      // " $ java " + JtlMain.class.getName()
            " $ jtl " + " [options ...] arg1 arg2 ...");
      System.out.println();
      System.out
            .println("  JTL is a language, library, tool and service for parsing, creating and transforming JSON data");
      System.out.println("  see: https://github.com/mdykman/jtl");
      System.out.println();

      Iterator<Option> oit = cl.getOptions().iterator();
      while(oit.hasNext()) {
         Option oo = oit.next();
         StringBuilder builder = new StringBuilder();
         builder.append("  -").append(oo.getOpt()).append(" --").append(oo.getLongOpt()).append("\t")
               .append(oo.getDescription());
         System.out.println(builder.toString());
      }
      System.out.println();

      System.out.println("  examples:");
      System.out.println("    $ jtl src/test/resources/group.jtl src/test/resources/generated.json");
      System.out.println("    $ jtl -x src/test/resources/group.jtl src/test/resources/generated.json");
      System.out.println("    $ jtl src/test/resources/re.jtl < src/test/resources/generated.json");
      System.out.println("    $ cat src/test/resources/generated.json | jtl src/test/resources/group.jtl");
      System.out.println("    $ jtl sample.jtl one.json two.json three.json");
      System.out.println("    $ cat  one.json two.json three.json | jtl -a sample.jtl");
      System.out.println("    $ jtl -e \"/people/count()\"  src/test/resources/generated.json");
      System.out.println();
   }

   public static void main(String[] args) {
      // TODO Auto-generated method stub
      JtlMain main = null;
      try {
         Options options = new Options();
         options.addOption(new Option("h", "help", false, "print this help message and exit"));
         options.addOption(new Option("c", "config", true, "specify a configuration file"));

         options.addOption(new Option("x", "jtl", true, "specify a jtl file"));
         options.addOption(new Option("d", "data", true, "specify input data (json file)"));
         options.addOption(new Option("D", "directory", true, "specify base directory (default:.)"));
         options.addOption(new Option("e", "expression", true, "exaluate an expression against inpt data"));

         options.addOption(new Option("s", "server", false, "run in server mode (default port:7718) * not implemented"));
         options.addOption(new Option("p", "port", true,
               "specify a port number (default:7718) * implies --server * not implemented"));

         options.addOption(new Option("k", "canconical", false, "output canonical JSON (ordered keys)"));
         options.addOption(new Option("n", "indent", true, "specify default indent level for output (default:3)"));
         options.addOption(new Option("q", "quote", false, "enforce quoting of all object keys (default:false)"));

         options.addOption(new Option("a", "array", false, "parse a sequence of json entities from the input stream, "
               + "assemble them into an array and process"));
         options.addOption(new Option("b", "batch", true,
               "gather n items from a sequence of JSON values from the input stream, processing them as an array"));

         options.addOption(new Option("z","null",false,"use null input data (cli-only)"));
         String s = System.getProperty("jtl.home");
         if(s == null)
            s = System.getProperty("JTL_HOME");
         File home = s == null ? null : new File(s);
         if(home == null) {
            System.err.println("unable to to determine JTL_HOME, using current directory");
            home = new File(".");
         }

         boolean help = false;
         File fconfig = null;
         File jtl = null;
         File fdata = null;
         Integer batch = null;
         int indent = 3;
         boolean dirSet = false;
         boolean array = false;
         boolean enquote = false;
         boolean useNull = false;
         File cexddir = new File(".");
         // File cdordir = new File(".");
         boolean serverMode = false;
         boolean canonical = false;
         String expr = null;
         int port = 7719; // default port

         CommandLineParser parser = new GnuParser();
         CommandLine cli;
         try {
            cli = parser.parse(options, args);
         } catch (ParseException e) {
            System.err.println("error parsing arguments: " + e.getLocalizedMessage());
            System.err.println();

            printHelp(options);
            System.exit(-1);
            throw new RuntimeException("exit didn't");
         }

         String oo;
         if(cli.hasOption('k') || cli.hasOption("canonical")) {
            canonical = true;
         }
         if(cli.hasOption('z') || cli.hasOption("null")) {
        	 useNull = true;
         }
         main = new JtlMain(canonical);
         if(cli.hasOption('D') || cli.hasOption("directory")) {
            oo = cli.getOptionValue('D');
            if(oo == null)
               oo = cli.getOptionValue("directory");
            cexddir = new File(oo);
            dirSet = true;
         }
         if(cli.hasOption('e') || cli.hasOption("expression")) {
            expr = cli.getOptionValue('e');
         }
         if(cli.hasOption('a') || cli.hasOption("array")) {
            array = true;
         }
         if(cli.hasOption('b') || cli.hasOption("batch")) {
            oo = cli.getOptionValue('b');
            if(oo == null)
               oo = cli.getOptionValue("batch");
            batch = Integer.parseInt(oo);
            if(batch == 0) {
               batch = null;
               // throw new
               // RuntimeException("don't know how to process a batch of 0");
            }
            array = true;
         }

         if(cli.hasOption('c') || cli.hasOption("config")) {
            oo = cli.getOptionValue('c');
            if(oo == null)
               oo = cli.getOptionValue("config");

            if(dirSet)
               fconfig = new File(cexddir, oo);
            else
               fconfig = new File(oo);
         }
         if(cli.hasOption('q') || cli.hasOption("quote")) {
            enquote = true;
         }
         if(cli.hasOption('x') || cli.hasOption("jtl")) {
            oo = cli.getOptionValue('c');
            oo = oo != null ? oo : cli.getOptionValue("jtl");
            jtl = new File(oo);
            if(!dirSet) {
               cexddir = jtl.getParentFile();
            }

         }
         if(cli.hasOption('p') || cli.hasOption("port")) {
            oo = cli.getOptionValue('p');
            oo = oo != null ? oo : cli.getOptionValue("port");
            port = Integer.parseInt(oo);
            serverMode = true;
         }
         if(cli.hasOption('s') || cli.hasOption("server")) {
            serverMode = true;
         }
         if(cli.hasOption('h') || cli.hasOption("help")) {
            printHelp(options);
            System.exit(0);
         }
         if(cli.hasOption('d') || cli.hasOption("data")) {
            oo = cli.getOptionValue('d');
            if(oo == null)
               oo = cli.getOptionValue("data");
            if(dirSet)
               fdata = new File(cexddir, oo);
            else
               fdata = new File(oo);
         }
         if(cli.hasOption('i') || cli.hasOption("indent")) {
            oo = cli.getOptionValue('i');
            if(oo == null)
               oo = cli.getOptionValue("indent");
            indent = Integer.parseInt(oo);
         }

         if(fconfig == null) {
            fconfig = main.searchConfig("config.json", cexddir, home);
         }
         main.setConfig(fconfig);

         List<String> argList = cli.getArgList();
         Iterator<String> argIt = argList.iterator();
         if(serverMode) {
            /*
            if(jtl == null) {
               if(cexddir == null) {
                  if(!argIt.hasNext()) {
                     System.err.println("no base directory or jtl file specified.");
                     printHelp(options);
                     System.exit(-1);
                  }
                  jtl = new File(argIt.next());
                  cexddir = jtl.getParentFile();
               } else {
                  System.err.println("server bound to directory " + cexddir.getAbsolutePath());
                  File def = new File(cexddir,"default.jtl");
                  if(!def.exists()) {
                     System.err.println("no default script found");
                  }
               }
            } else {
               if(cexddir == null) {
                  cexddir = jtl.getParentFile();
               }
            }
//            if(jtl == null) jtl = cexddir;
            JtlServer server = main.launchServer(cexddir,jtl, port);
            server.start();
            server.join();
            */
         } else {
            if(jtl == null) {
               // if not specified with a switch, the script must be the
               // first
               // name on the argument list
               if(argList.size() == 0 && expr == null) {
                  printHelp(options);
                  System.exit(-1);
                  throw new RuntimeException("could not determine input script");
               }
               // System.err.println("running script " + aa[0]);
               if(expr==null) jtl = new File(argIt.next());
            }
            if(expr == null && jtl == null) throw new RuntimeException("no program specified");
            InstructionFuture<JSON> inst = expr == null ? 
                  main.compile(jtl) : main.compile(expr);
                  String source = expr !=null ? "--expression" : jtl.getPath();
            PrintWriter pw = new PrintWriter(System.out);
            if(array) {
            	// declare all of argIt as arguments
               JSONArray arr = main.builder.array(null);
               jsonParser jp = main.createParser(System.in);
               int cc = 0;
               while(true) {
                  try {
                     JSON j = main.parse(jp);
                     if(j == null) {
                        break;
                     }
                     arr.add(j);
                     if((batch != null) && (++cc >= batch)) {
                        JSON result = main.execute(inst, source,arr, cexddir, fconfig,argIt);
                        result.write(pw, indent, enquote);
                        pw.flush();
                        cc = 0;
                        arr = main.builder.array(null);
                     }
                  } catch (IOException e) {
                     throw new RuntimeException("while reading sequence: " + e.getLocalizedMessage());
                  }
               }
               JSON result = main.execute(inst,source, arr, cexddir, fconfig,argIt);
               result.write(pw, indent, enquote);
               pw.flush();
            } else if(fdata != null) {
            	// declare all of argIt as arguments
               JSON data = main.parse(fdata);
               JSON result = main.execute(inst, source, data, cexddir, fconfig,argIt);
               result.write(pw, indent, enquote);
               // pw.flush();

            } else {
               if(!argIt.hasNext()) {
               	// empty arguments
                  JSON data = main.parse(System.in);
                  JSON result = main.execute(inst,source, data, cexddir, fconfig,argIt);
                  result.write(pw, indent, enquote);

               } else
                 // while(argIt.hasNext())
                  {
                     File f = new File(argIt.next());
                 	// declare all of argIt as arguments

                     JSON data = main.parse(f);
                     JSON result = main.execute(inst, source,data, cexddir, fconfig,argIt);
                     result.write(pw, indent, enquote);
                  }
            }
            pw.flush();
         }
      } catch (ExecutionException e) {
         System.err.println(e.report());
      } catch (Exception e) {
         if(e.getCause() instanceof ExecutionException) {
            System.err.println(((ExecutionException) e.getCause()).report());
         } else {
            e.printStackTrace();
         }
      } finally {
         try {
            main.shutdown();
         } catch (InterruptedException e) {
            System.err.println("error on shutdown: " + e.getLocalizedMessage());
            // e.printStackTrace();
         }
      }
   }

   public void setConfig(File f) throws IOException {
      config = f == null ? null : builder.parse(f);
   }

   public File searchConfig(String name, File... f) throws IOException {
      for(File ff : f) {
         File cc = new File(ff, name);
         if(cc.exists()) {
            return cc;
         }
      }
      return null;
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
/*
   public JtlServer launchServer(File base, File script, int port) {
      JtlServer server = new JtlServer(base,script, config, les, port);
      return server;
   }
*/
   public void shutdown() throws InterruptedException {
      les.shutdownNow();
      les.awaitTermination(2, TimeUnit.SECONDS);
   }

   public InstructionFuture<JSON> compile(File f) throws IOException {
      return compiler.parse(f);
   }

   public InstructionFuture<JSON> compile(String f) throws IOException {
      return compiler.parse("eval", f);
   }

   public JSON parse(File f) throws IOException {
      return builder.parse(f);
   }

   public JSON parse(Reader f) throws IOException {
      return builder.parse(f);
   }

   public JSON parse(InputStream f) throws IOException {
      return builder.parse(f);
   }

   public JSON parse(jsonParser p) throws IOException {
      return builder.parseSequence(p);
   }

   public jsonParser createParser(InputStream f) throws IOException {
      return builder.createParser(f);
   }

   public JSON execute(InstructionFuture<JSON> inst,String source, JSON data, File cwd, File config, Iterator<String> args) throws Exception {
      JSON c;
      if(config != null && config.exists()) {
         c = builder.parse(config);
      } else {
         c = builder.value();
      }
      AsyncExecutionContext<JSON> context = JtlCompiler.createInitialContext(data, c, cwd, builder, les);
      context.define("0", InstructionFutureFactory.value(builder.value(source), null));
      int cc = 1;
      JSONArray arr = builder.array(null);
      while(args.hasNext()) {
    	  JSON v = builder.value(args.next());
    	  context.define(Integer.toString(cc++), InstructionFutureFactory.value(v, null));
    	  arr.add(v);
      }
      context.define("#", InstructionFutureFactory.value(builder.value(arr.size()), null));
      context.define("@", InstructionFutureFactory.value(arr, null));
      context.setInit(true);
      // InstructionFuture<JSON> inst = compiler.parse(jtl);
      ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
      return j.get();
   }
}
