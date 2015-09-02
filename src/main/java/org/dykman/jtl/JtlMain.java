package org.dykman.jtl;

//import gnu.getopt.Getopt;
//import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
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
	ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors
			.newCachedThreadPool());

	JSON config = builder.value();

	public static void printHelp() {
		System.out.println("help!");
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	   JtlMain main = new JtlMain();
		try {
			String s = System.getProperty("jtl.home");
			File home;
			if (s == null) {
				home = new File(".");
			} else {
				home = new File(s);
			}

			Options options = new Options();
			options.addOption(new Option("c", "config", true,
					"specifiy a configuration file"));
			options.addOption(new Option("x", "jtl", true,
					"specifiy a jtl file"));
			options.addOption(new Option("p", "port", true,
					"specify a port (implies 'server' mode)"));
			options.addOption(new Option("d", "data", true,
					"specify input data (json file)"));
			options.addOption(new Option("h", "help", false,
					"print help message"));
			options.addOption(new Option("D", "directory", true,
					"specify base directory"));
			options.addOption(new Option("s", "server", false,
					"run in server mode (deafult port: 7718"));
			options.addOption(new Option("D", "directory", true,
					"specify base directory"));
			options.addOption(new Option("n", "indent", true,
					"specifiy default indent level for output (default is 3)"));
			options.addOption(new Option("a", "array", false, 
					"parse a sequence of json entities from the input stream and "
					+ "assemble them into an array"));

			int c;
			boolean help = false;
			File fconfig = null;
			File jtl = null;
			File fdata = null;
			int indent = 3;
			boolean array = false;
			File cwd = new File(".");
			boolean serverMode = false;
			int port = 7719; // default port

			CommandLineParser parser = new GnuParser();
			CommandLine cli = parser.parse(options, args);

			String oo;
			if (cli.hasOption('a') || cli.hasOption("array")) {
				array = true;
			}
			if (cli.hasOption('c') || cli.hasOption("config")) {
				oo = cli.getOptionValue('c');
//System.out.println("oo=" + oo);				
				if (oo == null)
					oo = cli.getOptionValue("config");

				fconfig = new File(oo);
//				fconfig = new File(cwd, oo);
			}
			if (cli.hasOption('x') || cli.hasOption("jtl")) {
				oo = cli.getOptionValue('c');
			//	if (oo == null)
					oo = cli.getOptionValue("jtl");
//				jtl = new File(cwd, oo);
				jtl = new File(oo);

			}
			if (cli.hasOption('s') || cli.hasOption("server")) {
				serverMode = true;
			}
			if (cli.hasOption('h') || cli.hasOption("help")) {
				help = true;
			}
			if (cli.hasOption('d') || cli.hasOption("data")) {
				oo = cli.getOptionValue('d');
				if (oo == null)
					oo = cli.getOptionValue("data");
//				fdata = new File(cwd, oo);
				fdata = new File(oo);
			}
			if (cli.hasOption('D') || cli.hasOption("directory")) {
				oo = cli.getOptionValue('D');
				if (oo == null)
					oo = cli.getOptionValue("directory");
				cwd = new File(cwd, oo);
			}
			if (cli.hasOption('i') || cli.hasOption("indent")) {
				oo = cli.getOptionValue('i');
				if (oo == null)
					oo = cli.getOptionValue("indent");
				indent = Integer.parseInt(oo);
			}

			if (help) {
				printHelp();
				System.exit(0);
			}

			
			if (fconfig != null)
				main.setConfig(fconfig);

			if (serverMode) {
				JtlServer server = main.launchServer(jtl, port);
				server.start();
				server.join();
			} else {
				if (jtl == null) {
					// if not specified with a switch, the script must be the
					// first
					// name on the argument list
					String[] aa = cli.getArgs();
					if (aa.length == 0) {
						throw new RuntimeException(
								"could not determine input script");
					}
//					System.err.println("running script " + aa[0]);
					jtl = new File(aa[0]);
					/*
					 * int n = g.getOptind(); String script = n >= args.length ?
					 * null : args[n]; if(script == null) { throw new
					 * RuntimeException("could not determine input script"); }
					 * jtl = new File(script);
					 */
				}
//            System.err.println("running file " + jtl.getAbsolutePath());
				InstructionFuture<JSON> inst = main.compile(jtl);
				PrintWriter pw = new PrintWriter(System.out);
				if(array) {
					JSONArray arr = main.builder.array(null);
					while(true) {
						InputStreamReader reader = new InputStreamReader(System.in);
						try {
						JSON j = main.parse(System.in);
						System.out.println("============================================!!!!!!!!!!!!!!==============");
						arr.add(j);
						
						} catch(IOException e) { 
							System.err.println("I hope THIS is antlr signaling EOF");
							e.printStackTrace();
							break;
						} catch(IllegalStateException e) {
							System.err.println("I hope this is antlr signaling EOF");
							e.printStackTrace();
							
							break;
						}
					}
					JSON result = main.execute(inst, arr, cwd, fconfig);
					result.write(pw, indent, false);
					pw.flush();
				} else	if (fdata != null) {
					JSON data = main.parse(fdata);
					JSON result = main.execute(inst, data, cwd, fconfig);
					result.write(pw, indent, false);
					pw.flush();

				} else {
					String[] aa = cli.getArgs();
					for (String a : aa) {
						File f = new File(a);
						JSON data = main.parse(f);
						JSON result = main.execute(inst, data, cwd, fconfig);
						result.write(pw, indent, false);
						pw.flush();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		   try {
            main.shutdown();
         } catch (InterruptedException e) {
            System.err.println("error un shutdown: " + e.getLocalizedMessage());
//            e.printStackTrace();
         }
		}
	}

	public void setConfig(File f) throws IOException {
		config = builder.parse(f);
	}

	public JSONObject getConfig(File f) throws IOException {
		JSON c;
		if (f != null && f.exists()) {
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

	public JSON execute(InstructionFuture<JSON> inst, JSON data, File cwd,
			File config) throws Exception {
		// JSON d = builder.parse(data);
		JSON c;
		if (config != null && config.exists()) {
			c = builder.parse(config);
		} else {
			c = builder.value();
		}
		AsyncExecutionContext<JSON> context = JtlCompiler.createInitialContext(
				data, c, cwd, builder, les);
		// InstructionFuture<JSON> inst = compiler.parse(jtl);
		ListenableFuture<JSON> j = inst.call(context,
				Futures.immediateFuture(data));
		return j.get();
	}
}
