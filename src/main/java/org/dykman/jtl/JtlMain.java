package org.dykman.jtl;

//import gnu.getopt.Getopt;
//import gnu.getopt.LongOpt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

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
import org.dykman.jtl.modules.JdbcModule;
import org.dykman.jtl.server.JtlServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@SuppressWarnings("deprecation")
public class JtlMain {

	static final String JTL_VERSION = "0.9.7";

	final JSONBuilder builder;
	InstructionFutureFactory factory = new InstructionFutureFactory();
	JtlCompiler compiler;
	ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

	JSONObject config;
	File configFile = null;
	static boolean verbose = false;
	static Logger logger;


	public JtlMain(File jtlBase,File conf, boolean canonical) throws IOException {
		builder = new JSONBuilderImpl(canonical);
		compiler = new JtlCompiler(builder, false, false, false);
		
		JSONObject bc = (JSONObject) builder.parse(new File(jtlBase, "conf/config.json"));
		if (conf != null) {
			if (!conf.exists()) {
				throw new RuntimeException("unable to locate specified config: " + conf.getAbsolutePath());
			}
			bc = bc.overlay((JSONObject) builder.parse(conf));
		}
		
		

		config = bc; // initial config value
		config.put("server-mode", builder.value(false));
		configFile = conf;
	}

	public static void setVerbose(boolean b) {
		verbose = b;
	}
	public static void printHelp(Options cl) {
		System.out.println(" $ jtl " + " [options ...] [arg1 ... ]");
		System.out.println();
		System.out.println("  JTL is a language, library, tool and service for parsing, creating and transforming JSON data");
		System.out.println("    see: https://github.com/mdykman/jtl/README.md");
		System.out.println();
		System.out.println("  options:");
		System.out.println();

		Iterator<Option> oit = cl.getOptions().iterator();
		while (oit.hasNext()) {
			Option oo = oit.next();
			StringBuilder builder = new StringBuilder();
			builder.append("  -").append(oo.getOpt()).append(" --").append(oo.getLongOpt()).append("\t")
					.append(oo.getDescription());
			System.out.println(builder.toString());
		}
		System.out.println();

		System.out.println("  examples:");
		System.out.println();
		
		System.out.println("    $ jtl src/test/resources/group.jtl src/test/resources/generated.json");
		System.out.println("    $ jtl -x src/test/resources/group.jtl -o output.json src/test/resources/generated.json");
		System.out.println("    $ jtl src/test/resources/re.jtl < src/test/resources/generated.json");
		System.out.println("    $ cat src/test/resources/generated.json | jtl src/test/resources/group.jtl");
		System.out.println("    $ jtl sample.jtl one.json two.json three.json");
		System.out.println("    $ cat  one.json two.json three.json | jtl -a sample.jtl");
		System.out.println("    $ jtl -e \"/people/count()\"  src/test/resources/generated.json");
		System.out.println();
	}

	public static void main(String[] args) {
		JtlMain main = null;
		try {
			
			Options options = new Options();
			options.addOption(new Option("h", "help", false, "print this help message and exit"));
			options.addOption(new Option("V", "version", false, "print jtl version"));
			options.addOption(new Option("c", "config", true, "specify a configuration file"));
			options.addOption(new Option("i", "init", true, "specify an init script"));
			options.addOption(new Option("v", "verbose", false, "generate verbose output to stderr"));
			options.addOption(new Option("l", "log", true, "set the log level, one of: trace, debug, info, warn, or error (default:warn)"));

			
			options.addOption(new Option("x", "jtl", true, "specify a jtl file"));
			options.addOption(new Option("d", "data", true, "specify an input json file"));
			options.addOption(new Option("D", "dir", true, "specify base directory (default:.)"));
			options.addOption(new Option("e", "expr", true, "evaluate an expression against input data"));
			options.addOption(new Option("o", "output", true, "specify an output file (cli-only)"));

			options.addOption(
					new Option("s", "server", false, "run in server mode (default port:7718)"));
			options.addOption(new Option("p", "port", true,
					"specify a port number (default:7718) * implies --server"));

			options.addOption(
					new Option("B", "binding", true, "bind network address * implies --server (default:127.0.0.1)"));


			options.addOption(new Option("k", "canon", false, "output canonical JSON (ordered keys)"));
			options.addOption(new Option("n", "indent", true, "specify default indent level for output (default:3)"));
			options.addOption(new Option("q", "quote", false, "enforce quoting of all object keys (default:false)"));

			options.addOption(
					new Option("a", "array", false, "parse a sequence of json entities from the input stream, "
							+ "assemble them into an array and process"));
			options.addOption(new Option("b", "batch", true,
					"gather n items from a sequence of JSON values from the input stream, processing them as an array"));

			options.addOption(new Option("z", "null", false, "use null input data (cli-only)"));
			String s = System.getProperty("jtl.home");
			if (s == null)
				s = System.getProperty("JTL_HOME");
			File home = s == null ? null : new File(s);
			if (home == null) {
				System.err.println("unable to to determine JTL_HOME, using current directory");
				home = new File(".");
			}

			File fconfig = null;
			File jtl = null;
			File fdata = null;
			File init = null;
			File output = null;
			Integer batch = null;
			String logLevel = "warn";
			int indent = 3;
			boolean dirSet = false;
			boolean verbose = false;
			boolean array = false;
			boolean enquote = false;
			boolean useNull = false;
			File cexddir = null;
			// File cdordir = new File(".");
			boolean serverMode = false;
			boolean canonical = false;
			String expr = null;
			int port = 7718; // default port
			String bindAddress = null;

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
			
			if(cli.hasOption('v')) {
				verbose = true;
			}
			
			if (cli.hasOption('l')) {
				logLevel = cli.getOptionValue('l'); 
			} else if(verbose) {
				logLevel="info";

			}
			System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", logLevel);
			logger = LoggerFactory.getLogger(JdbcModule.class);
			

			String oo;
			if (cli.hasOption('p') || cli.hasOption("port")) {
				oo = cli.getOptionValue('p');
				oo = oo != null ? oo : cli.getOptionValue("port");
				port = Integer.parseInt(oo);
				logger.info("listening on port " + port);
				serverMode = true;
			}
			if(cli.hasOption('V')) {
				System.out.print("jtl version " + JTL_VERSION);
				System.out.println(" - see https://github.com/mdykman/jtl");
				System.exit(0);
			}
			if (cli.hasOption('s') || cli.hasOption("server")) {
				serverMode = true;
			}
			
			if (cli.hasOption('B') || cli.hasOption("binding")) {
				bindAddress = cli.getOptionValue('B');
				logger.info("binding to interface " + bindAddress);
				serverMode = true;
			}
			if (cli.hasOption('o') || cli.hasOption("output")) {
				oo = cli.getOptionValue('o');
				output = new File(oo);
				logger.info("writing output to " + output.getPath());
			}
			if (cli.hasOption('z') || cli.hasOption("null")) {
				logger.info("using null input data");
				useNull = true;
			}
			if (cli.hasOption('i') || cli.hasOption("init")) {
				init = new File(cli.getOptionValue('i'));
				logger.info("using init script: " + init.getPath());
			}
			if (cli.hasOption('k') || cli.hasOption("canon")) {
				canonical = true;
			}
			if (cli.hasOption('c') || cli.hasOption("config")) {
				oo = cli.getOptionValue('c');
				if (oo == null)
					oo = cli.getOptionValue("config");

	//			if (dirSet)
	//				fconfig = new File(cexddir, oo);
	//			else
					fconfig = new File(oo);
					logger.info("using optional configuration " + fconfig.getPath());
			}			
			
			main = new JtlMain(home,fconfig, canonical);
			if (cli.hasOption('D') || cli.hasOption("dir")) {
				oo = cli.getOptionValue('D');
				if (oo == null)
					oo = cli.getOptionValue("directory");
				cexddir = new File(oo);
				dirSet = true;
			}
			if (cli.hasOption('e') || cli.hasOption("expr")) {
				expr = cli.getOptionValue('e');
			}
			if (cli.hasOption('a') || cli.hasOption("array")) {

				array = true;
				logger.info("reading a sequence of JSON entities from stdin");
			}
			if (cli.hasOption('b') || cli.hasOption("batch")) {
				oo = cli.getOptionValue('b');
				if (oo == null)
					oo = cli.getOptionValue("batch");
				batch = Integer.parseInt(oo);
				if (batch == 0) {
					batch = null;
					// throw new
					// RuntimeException("don't know how to process a batch of
					// 0");
				}
				if(batch == 0) 
					logger.info("reading a sequence of JSON entities from stdin");
				else 
					logger.info("reading a sequence of JSON entities in batched of " + oo + "from stdin");

				array = true;
			}


			if (cli.hasOption('q') || cli.hasOption("quote")) {
				logger.info("force key enquoting ");
				enquote = true;
			}
			if (cli.hasOption('x') || cli.hasOption("jtl")) {
				oo = cli.getOptionValue('c');
				oo = oo != null ? oo : cli.getOptionValue("jtl");
				jtl = new File(oo);

			}
			if (cli.hasOption('h') || cli.hasOption("help")) {
				printHelp(options);
				System.exit(0);
			}
			if (cli.hasOption('d') || cli.hasOption("data")) {
				oo = cli.getOptionValue('d');
				if (oo == null)
					oo = cli.getOptionValue("data");
//				if (dirSet)
//					fdata = new File(cexddir, oo);
//				else
					fdata = new File(oo);
			}
			if (cli.hasOption('i') || cli.hasOption("indent")) {
				oo = cli.getOptionValue('i');
				if (oo == null)
					oo = cli.getOptionValue("indent");
				indent = Integer.parseInt(oo);
			}
			


			if(jtl!=null && cexddir == null) {
				jtl = jtl.getAbsoluteFile();
				cexddir = jtl.getParentFile();
			}
			
			if(serverMode) logger.info("running in server mode");
			else logger.info("running in cli mode");

			
			List<String> argList = cli.getArgList();
			Iterator<String> argIt = argList.iterator();
			if (serverMode) {

				if (jtl == null) {
					if (cexddir == null) {
						if (!argIt.hasNext()) {
							System.err.println("no base directory or jtl file specified.");
							printHelp(options);
							System.exit(-1);
						}
						jtl = new File(argIt.next());
						jtl = jtl.getAbsoluteFile();
						if(cexddir ==null) cexddir = jtl.getParentFile();
					}
				} else {
					if (cexddir == null) {
						cexddir = jtl.getParentFile();
					}
				}
				logger.info("using base directory " + cexddir.getPath());

				JtlServer server = main.launchServer(home, cexddir, init, jtl, fconfig, bindAddress, port, canonical);
				server.start();
				server.join();

			} else {
				JtlMain.setVerbose(verbose);
				if (jtl == null) {
					// if not specified with a switch, the script must be the
					// first
					// name on the argument list
					if (argList.size() == 0 && expr == null) {
						printHelp(options);
						System.exit(-1);
						throw new RuntimeException("could not determine input script");
					}
					if (expr == null) {
						jtl = new File(argIt.next());
						jtl = jtl.getAbsoluteFile();
						cexddir = jtl.getParentFile();
					}
				}
				logger.info("using base directory " + cexddir.getPath());
				
				if (expr == null && jtl == null)
					throw new RuntimeException("no program specified");
				
				if(verbose) {
					if(expr!=null) {
						logger.info("evaluating expression: " + expr);
					} else {
						logger.info("evaluating file: " + jtl.getAbsolutePath());
					}
				}
				InstructionFuture<JSON> inst = expr == null ? main.compile(jtl) : main.compile(expr);
				String source = expr != null ? "--expr" : jtl.getPath();
				PrintWriter pw;
				if(output == null) {
					pw = new PrintWriter(System.out);
				} else {
					pw = new PrintWriter(output, "UTF-8");
				}
				if (array) {
					// declare all of argIt as arguments
					JSONArray arr = main.builder.array(null);
					jsonParser jp = main.createParser(System.in);
					int cc = 0;
					while (true) {
						try {
							JSON j = main.parse(jp);
							if (j == null) {
								break;
							}
							arr.add(j);
							if ((batch != null) && (++cc >= batch)) {
								JSON result = main.execute(inst, source, arr, cexddir, argIt);
								result.write(pw, indent, enquote);
								pw.flush();
								cc = 0;
								arr = main.builder.array(null);
							}
						} catch (IOException e) {
							throw new RuntimeException("while reading sequence: " + e.getLocalizedMessage());
						}
					}
					JSON result = main.execute(inst, source, arr, cexddir, argIt);
					result.write(pw, indent, enquote);
					pw.flush();
				} else if (fdata != null) {
					// declare all of argIt as arguments
					JSON data = main.parse(fdata);
					JSON result = main.execute(inst, source, data, cexddir, argIt);
					result.write(pw, indent, enquote);
				} else {
					if (useNull) {
						JSON data = main.empty();
						JSON result = main.execute(inst, source, data, cexddir,  argIt);
						result.write(pw, indent, enquote);
					} else if (!argIt.hasNext()) {
						// empty arguments
						JSON data = main.parse(System.in);
						JSON result = main.execute(inst, source, data, cexddir, argIt);
						result.write(pw, indent, enquote);

					} else {
						File f = new File(argIt.next());
						// declare all of argIt as arguments

						JSON data = main.parse(f);
						JSON result = main.execute(inst, source, data, cexddir, argIt);
						result.write(pw, indent, enquote);
					}
				}
				pw.flush();
				if(output != null) pw.close();
			}
		} catch (ExecutionException e) {
			System.err.println(e.report());
			if(verbose) e.printStackTrace();
		} catch (Throwable e) {
			if (e.getCause() instanceof ExecutionException) {
				System.err.println(((ExecutionException) e.getCause()).report());
			} else {
				Throwable ee = e.getCause() == null ? e : e.getCause();
				System.err.println("an error occured: " + ee.getLocalizedMessage());
			}
			if(verbose) e.printStackTrace();
		} finally {
			try {
				main.shutdown();
			} catch (InterruptedException e) {
				System.err.println("error on shutdown: " + e.getLocalizedMessage());
			}
		}
	}/*

	public void setConfig(File f) throws IOException {
		try {
		config = f == null ? null : (JSONObject) builder.parse(f);
		} catch(ClassCastException e) {
			throw new IOException("config at " +f.getAbsolutePath() + "is not a json object",e);
		}
	}

	public File searchConfig(String name, File... f) throws IOException {
		for (File ff : f) {
			File cc = new File(ff, name);
			if (cc.exists()) {
				return cc;
			}
		}
		return null;
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
*/
	public JtlServer launchServer(File jtlBase, File serverBase, File init, File script, File config,
			String bindAddress, int port, boolean canonical) throws IOException {
		// public JtlServer(File jtlBase,File serverBase, File init, File
		// script, File config, int port,boolean canonical) throws IOException {

		JtlServer server = new JtlServer(jtlBase, serverBase, init, script, config, bindAddress, port, canonical);
		// new JtlServer(base,script, configFile, port);
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

	public JSON parse(jsonParser p) throws IOException {
		return builder.parseSequence(p);
	}

	public jsonParser createParser(InputStream f) throws IOException {
		return builder.createParser(f);
	}

	public JSON empty() {
		return builder.value();
	}

	public JSON execute(InstructionFuture<JSON> inst, String source, JSON data, File cwd,
			Iterator<String> args) throws Exception {
		;
//		if (config != null && config.exists()) {
//			c = builder.parse(config);
//		} else {
//			c = builder.value();
//		}
		AsyncExecutionContext<JSON> context = JtlCompiler.createInitialContext(data, config, cwd, builder, les);
		context.setInit(true);
		context.define("0", InstructionFutureFactory.value(builder.value(source), SourceInfo.internal("cli")));
		int cc = 1;
		JSONArray arr = builder.array(null);
		if(args!=null) while (args.hasNext()) {
			JSON v = builder.value(args.next());
			context.define(Integer.toString(cc++), InstructionFutureFactory.value(v, SourceInfo.internal("cli")));
			arr.add(v);
		}
		ListenableFuture<JSON> dd= Futures.immediateCheckedFuture(data);
		
//		context.define("#", InstructionFutureFactory.value(builder.value(arr.size()), SourceInfo.internal("cli")));
		context.define("@", InstructionFutureFactory.value(arr, SourceInfo.internal("cli")));
		context.define("_", InstructionFutureFactory.value(data, SourceInfo.internal("cli")));

		context = context.createChild(false, false, dd,  SourceInfo.internal("cli"));
		context.setRuntime(true);

		ListenableFuture<JSON> j = inst.call(context, dd);
		return j.get();
	}
}
