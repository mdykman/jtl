package org.dykman.jtl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.FileReader;
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
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.modules.ModuleLoader;
import org.dykman.jtl.operator.FutureInstruction;
import org.dykman.jtl.operator.FutureInstructionFactory;
import org.dykman.jtl.server.JtlServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import jline.console.ConsoleReader;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("deprecation")
public class JtlMain {

	static final String JTL_VERSION = JtlVersion.JTL_VERSION;
	boolean yamlIn;
	boolean yamlOut;

	final JSONBuilder builder;
	FutureInstructionFactory factory = new FutureInstructionFactory();
	JtlCompiler compiler;
	static ListeningExecutorService les;

	JSONObject config;
	File configFile = null;
	static boolean verbose = false;
	static Logger logger;
	Yaml yaml = null;

	public JtlMain(File jtlBase, File conf, boolean canonical,boolean yamlIn, boolean yamlOut) throws IOException {
		this.yamlIn = yamlIn;
		this.yamlOut = yamlOut;
		if(yamlIn || yamlOut) yaml = new Yaml();
		builder = new JSONBuilderImpl(canonical);
		compiler = new JtlCompiler(builder);

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
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					les.shutdown();
					les.awaitTermination(2000L, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					logger.error("failed to shutdown ExecutorService within 2000 ms. Shutting down hard.");
					les.shutdownNow();
				}
			}
		});
	}

	public static void setVerbose(boolean b) {
		verbose = b;
	}

	public void setSyntaxCheck(boolean b) {
		compiler.setSyntaxCheck(b);
	}

	public JSONArray args(Iterator<String> sit) {
		JSONArray a = builder.array(null);
		while (sit.hasNext()) {
			a.add(builder.value(sit.next()));
		}
		return a;
	}

	public static void printHelp(Options cl) {
		System.out.println("JSON Transformation Language " + JTL_VERSION);
		System.out.println(" $ jtl " + " [options ...] [arg1 ... ]");
		System.out.println();
		System.out.println(
				"  JTL is a language, library, tool and service for parsing, creating and transforming JSON data");
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
	}

	public static void main(String[] args) {
		JtlMain main = null;
		try {

			Options options = new Options();
			options.addOption(new Option("Y", "yaml", true, "specify YAML input and/or output (cli-only) *not implemented" ));
			options.addOption(new Option("h", "help", false, "print this help message and exit"));
			options.addOption(new Option("V", "version", false, "print jtl version"));
			options.addOption(new Option("c", "config", true, "specify a configuration file"));
			options.addOption(new Option("i", "init", true, "specify an init script"));
			options.addOption(new Option("v", "verbose", false, "generate verbose output to stderr"));
			options.addOption(new Option("l", "log", true,
					"set the log level, one of: trace, debug, info, warn, or error (default:warn)"));

			options.addOption(new Option("x", "jtl", true, "specify a jtl file"));
			options.addOption(new Option("d", "data", true, "specify an input json file"));
			options.addOption(new Option("D", "dir", true, "specify base directory (default:.)"));
			options.addOption(new Option("e", "expr", true, "evaluate an expression against input data"));
			options.addOption(new Option("R", "resources", true, "specify a resource directory"));
			options.addOption(new Option("o", "output", true, "specify an output file (cli-only)"));
			options.addOption(new Option("r", "repl", false, "open an interactive console (not impl)"));
			options.addOption(new Option("t", "threads", true, "set the paralellism level (default:20)"));

			options.addOption(new Option("s", "server", false, "run in server mode (default port:7718)"));
			options.addOption(new Option("p", "port", true, "specify a port number (default:7718) * implies --server"));

			options.addOption(
					new Option("b", "bind", true, "bind network address * implies --server (default:0.0.0.0)"));

			options.addOption(new Option("k", "canonical", false, "output 'canonical' JSON (enforce ordered keys)"));
			options.addOption(new Option("n", "indent", true,
					"specify default indent level for output (cli default:3, server default:0)"));
			options.addOption(new Option("Q", "dequote", false, "allow well formed keys to be unquoted"));

			options.addOption(new Option("S", "syntax-check", false, "syntax check only, do not execute"));

			options.addOption(
					new Option("a", "array", false, "parse a sequence of json entities from the input stream, "
							+ "assemble them into an array and process"));
			options.addOption(new Option("B", "batch", true,
					"gather n items from a sequence of JSON values from the input stream, processing them as an array"));

			options.addOption(new Option("z", "null", false, "use null input data (cli-only)"));

			File fconfig = null;
			File resources = null;
			File jtl = null;
			File fdata = null;
			File init = null;
			File output = null;
			Integer batch = null;
			String logLevel = "warn";
			int indent = 3;

			boolean verbose = false;
			boolean array = false;
			boolean enquote = true;
			boolean useNull = false;
			boolean yamlIn = false;
			boolean yamlOut = false;
			File cexddir = null;

			boolean serverMode = false;
			boolean replMode = false;
			boolean canonical = false;
			String expr = null;
			int port = 7718; // default port
			String bindAddress = null;
			int threads = 20;

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

			if (cli.hasOption('Y')) {
				String y = cli.getOptionValue('Y');	
				yamlIn = y.contains("i") || y.contains("I");
				yamlOut = y.contains("o") || y.contains("O");
				verbose = true;
			}
			if (cli.hasOption('v')) {
				verbose = true;
			}
			if (cli.hasOption('l')) {
				logLevel = cli.getOptionValue('l');
			} else if (verbose) {
				logLevel = "info";
			}
			ConsoleAppender console = new ConsoleAppender();
			String PATTERN = "%d [%t|%p|%c{2}|%C{1}] %m%n";
			console.setLayout(new PatternLayout(PATTERN));
			console.setThreshold(Level.toLevel(logLevel, Level.ERROR));
			console.activateOptions();
			console.setWriter(new OutputStreamWriter(System.err, "UTF-8"));

			org.apache.log4j.Logger.getRootLogger().addAppender(console);
			org.apache.log4j.Logger.getRootLogger().setLevel(Level.toLevel(logLevel));

			logger = LoggerFactory.getLogger(JtlMain.class);
			String s = System.getProperty("jtl.home");
			if (s == null)
				s = System.getProperty("JTL_HOME");
			File home = s == null ? null : new File(s);
			if (home == null) {
				logger.info("unable to to determine JTL_HOME, using current directory");
				home = new File(".");
			}

			logger.info("starting thread-pool with a concurrency of " + threads);

			String oo;

			if (cli.hasOption('t')) {
				threads = Integer.parseInt(cli.getOptionValue('t'));
			}
			les = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(threads));

			if (cli.hasOption('r')) {
				logger.error("repl is not implemented");

				// not yet supported
				replMode = true;
			}
			
			if(cli.hasOption('R')) {
				resources = new File(cli.getOptionValue('R'));
				if(!resources.canRead()) {
					logger.error(String.format("unable to read resources directory %s",resources.getCanonicalFile()));
					System.exit(1);
				}
			}

			if (cli.hasOption('p') || cli.hasOption("port")) {
				oo = cli.getOptionValue('p');
				oo = oo != null ? oo : cli.getOptionValue("port");
				port = Integer.parseInt(oo);
				serverMode = true;
			}
			if (cli.hasOption('V')) {
				System.out.print("jtl version " + JTL_VERSION);
				System.out.println(" - see https://github.com/mdykman/jtl");
				System.exit(0);
			}
			if (cli.hasOption('s') || cli.hasOption("server")) {
				serverMode = true;
			}

			if (cli.hasOption('b') || cli.hasOption("bind")) {
				bindAddress = cli.getOptionValue('b');
				serverMode = true;
			}
			if (serverMode) {
				logger.info(String.format("server enable. binding to interface %s:$d", bindAddress, port));
			}
			if (cli.hasOption('D') || cli.hasOption("dir")) {
				oo = cli.getOptionValue('D');
				if (oo == null)
					oo = cli.getOptionValue("directory");
				cexddir = new File(oo);
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
			if (cli.hasOption('k') || cli.hasOption("canonical")) {
				canonical = true;
			}
			if (cli.hasOption('c') || cli.hasOption("config")) {
				oo = cli.getOptionValue('c');
				if (oo == null)
					oo = cli.getOptionValue("config");

				// if (dirSet)
				// fconfig = new File(cexddir, oo);
				// else
				fconfig = new File(oo);
				logger.info("using optional configuration " + fconfig.getPath());
			}

			main = new JtlMain(home, fconfig, canonical,yamlIn,yamlOut);
			if (cli.hasOption('S')) {
				main.setSyntaxCheck(true);
				useNull = true;
			}
			if (cli.hasOption('e') || cli.hasOption("expr")) {
				expr = cli.getOptionValue('e');
			}
			if (cli.hasOption('a') || cli.hasOption("array")) {

				array = true;
				logger.info("reading a sequence of JSON entities from stdin");
			}
			if (cli.hasOption('B') || cli.hasOption("batch")) {
				oo = cli.getOptionValue('B');
				if (oo == null)
					oo = cli.getOptionValue("batch");
				batch = Integer.parseInt(oo);
				if (batch == 0) {
					batch = null;
					// throw new
					// RuntimeException("don't know how to process a batch of
					// 0");
				}
				if (batch == 0)
					logger.info("reading a sequence of JSON entities from stdin");
				else
					logger.info("reading a sequence of JSON entities in batched of " + oo + "from stdin");

				array = true;
			}

			if (cli.hasOption('Q') || cli.hasOption("dequote")) {
				logger.info("force key enquoting ");
				enquote = false;
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
				// if (dirSet)
				// fdata = new File(cexddir, oo);
				// else
				fdata = new File(oo);
			}
			if (cli.hasOption('n') || cli.hasOption("indent")) {
				oo = cli.getOptionValue('n');
				if (oo == null)
					oo = cli.getOptionValue("indent");
				indent = Integer.parseInt(oo);
			}

			if (jtl != null && cexddir == null) {
				jtl = jtl.getAbsoluteFile();
				cexddir = jtl.getParentFile();
			}

			if (serverMode)
				logger.info("running in server mode");
			else
				logger.info("running in cli mode");

			List<String> argList = cli.getArgList();
			Iterator<String> argIt = argList.iterator();
			if (serverMode) {

				if (jtl == null) {
					if (cexddir == null) {
						if (!argIt.hasNext()) {
							cexddir = new File(".");
						} else {
							jtl = new File(argIt.next());
							jtl = jtl.getAbsoluteFile();
							if (cexddir == null)
								cexddir = jtl.getParentFile();
						}
					}
				} else {
					if (cexddir == null) {
						cexddir = jtl.getParentFile();
					}
				}
				cexddir = cexddir.getCanonicalFile();
				logger.info("using base directory " + cexddir.getPath());

				JtlServer server = main.launchServer(home, cexddir,resources, init, jtl, fconfig, bindAddress, port, canonical);
				server.start();
				server.join();

			} else {
				// cli
				JtlMain.setVerbose(verbose);
				if (jtl == null) {
					// if not specified with a switch, the script must be the
					// first name on the argument list
					if (argList.size() == 0 && expr == null) {
						printHelp(options);
						System.exit(-1);
						throw new RuntimeException("could not determine input script");
					}
					if (expr == null) {
						jtl = new File(argIt.next());
						jtl = jtl.getAbsoluteFile();
						if (cexddir != null)
							cexddir = jtl.getParentFile();
					}
				}
				if (cexddir == null) {
					cexddir = new File(".").getCanonicalFile();
				}
				logger.info("using base directory " + cexddir.getPath());

				if (expr == null && jtl == null)
					throw new RuntimeException("no program specified");

				if (verbose) {
					if (expr != null) {
						logger.info("evaluating expression: " + expr);
					} else {
						logger.info("evaluating file: " + jtl.getAbsolutePath());
					}
				}
				FutureInstruction<JSON> inst = null;
				PrintWriter pw;
				String source;
				try {
					inst = expr == null ? main.compile(jtl) : main.compile(expr);
					source = expr != null ? "--expr" : jtl.getPath();
					if (output == null) {
						pw = new PrintWriter(System.out);
					} else {
						pw = new PrintWriter(output, "UTF-8");
					}
				} catch (JtlParseException e) {
					System.err.println("----------------------------------------------");
					System.err.println(e.report());
					logger.error(e.report());
					return;
				} catch (Exception e) {
					System.err.println(e.getLocalizedMessage());
					logger.error(e.getLocalizedMessage());
					return;
				}
				JSON result;
				JSON data;
				if (array) {
					// declare all of argIt as arguments
					JSONArray bargs = main.args(argIt);

					jsonParser jp = main.createParser(System.in);
					int cc = 0;
					while (true) {
						try {
							JSON j = main.parse(jp);
							if (j == null) {
								break;
							}
							if ((batch != null) && (++cc >= batch)) {
								JSON r = main.execute(inst, source, init, j, cexddir, bargs);
								r.write(pw, indent, enquote);
								cc = 0;
							}
						} catch (IOException e) {
							throw new RuntimeException("while reading sequence: " + e.getLocalizedMessage());
						}
					}
					pw.flush();
					return;
				} else if (fdata != null) {
					data = main.parse(fdata);
				} else {
					if (useNull) {
						data = main.empty();
					} else if (!argIt.hasNext()) {
						try {
							logger.info("reading std input");
							data = main.parse(System.in);
						} catch(Exception e) {
							throw new RuntimeException("failed to read stdin. " + e.getLocalizedMessage());
						}
					} else {
						File f = new File(argIt.next());
						data = main.parse(f);
					}
				}

				if (replMode) {
					logger.info("starting console");
					AsyncExecutionContext<JSON> context = main.createInitialContext(data, cexddir, init);
					ConsoleReader cons = new ConsoleReader(System.in, System.out);
					cons.setPrompt("jtl>");
					cons.addCompleter(new Completer() {
						@Override
						public int complete(String buffer, int cursor, List<CharSequence> candidates) {
							return 0;
						}
					});
					cons.setCompletionHandler(new CandidateListCompletionHandler());
					JSONArray aa = main.args(argIt);
					String line = cons.readLine();
					while (line != null) {
						try {
							FutureInstruction<JSON> fit = main.compile(line);
							JSON jres = main.execute(fit, context, source, init, data, cexddir, aa);
							jres.write(pw, indent, enquote);
						} catch (Exception ee) {
							pw.println("  error: " + ee.getLocalizedMessage());
						}
						line = cons.readLine();
						if (line == null) {
							break;
						}
					}
					pw.close();
					return;
				} else {
					result = main.execute(inst, source, init, data, cexddir, main.args(argIt));
					result.write(pw, indent, enquote);
					pw.flush();
					if (output != null)
						pw.close();
				}
			}
		} catch (JtlParseException e) {
			System.err.println(e.report());
			if (verbose)
				e.printStackTrace();
		} catch (ExecutionException e) {
			System.err.println(e.report());
			if (verbose)
				e.printStackTrace();
		} catch (Throwable e) {
			if (e.getCause() instanceof ExecutionException) {
				System.err.println(((ExecutionException) e.getCause()).report());
			} else {
				Throwable ee = e.getCause() == null ? e : e.getCause();
				logger.error(ee.getClass().getName() + " - an error occured: " + ee.getLocalizedMessage());
			}
			if (verbose)
				e.printStackTrace();
		} finally {
			try {
				main.shutdown();
			} catch (InterruptedException e) {
				System.err.println("error on shutdown: " + e.getLocalizedMessage());
			}
		}
	}

	public JtlServer launchServer(File jtlBase, File serverBase, File resources,File init, File script, File config,
			String bindAddress, int port, boolean canonical) throws IOException {

		JtlServer server = new JtlServer(jtlBase, serverBase,resources, init, script, config, bindAddress, port, canonical);
		return server;
	}

	public void shutdown() throws InterruptedException {
		try {
			les.shutdown();
			les.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.warn("ExecutorService did not shutdown gracefully within 2 seconds.  Terminating.");
			les.shutdownNow();
		}
	}

	public FutureInstruction<JSON> compile(File f) throws IOException {
		return compiler.parse(f);
	}

	public FutureInstruction<JSON> compile(String f) throws IOException {
		return compiler.parse("eval", f);
	}

	public JSON parse(File f) throws IOException {
		if(!f.exists()) {
			throw new IOException(String.format("can not find input file %s",f.getAbsolutePath()));
		}
		if(yamlIn) {
			Object o = yaml.load(new FileReader(f));
			throw new UnsupportedOperationException("parsing data with YAML not yet supported");
		} else {
			return builder.parse(f);
		}
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
		return JSONBuilderImpl.NULL;
	}

	AsyncExecutionContext<JSON> createInitialContext(JSON data, File cwd, File init)
			throws ExecutionException, IOException {
		return compiler.createInitialContext(data, config, cwd, init, builder, les);
	}

	public JSON execute(FutureInstruction<JSON> inst, String source, File init, JSON data, File cwd, JSONArray args)
			throws Exception {
		AsyncExecutionContext<JSON> context = createInitialContext(data, cwd, init);
		ModuleLoader ml = ModuleLoader.getInstance(context.currentDirectory(), context.builder(), config);
		ml.loadAuto(context, false);
		return execute(inst, context, source, init, data, cwd, args);
	}

	public JSON execute(FutureInstruction<JSON> inst, AsyncExecutionContext<JSON> context, String source, File init,
			JSON data, File cwd, JSONArray args) throws Exception {
		ListenableFuture<JSON> dd = Futures.immediateCheckedFuture(data);
		context = context.createChild(false, false, dd, SourceInfo.internal("runtime"));
		context.setRuntime(true);
		context.define("0", FutureInstructionFactory.value(builder.value(source), SourceInfo.internal("source")));
		int cc = 1;
		JSONArray arr = args;
		if (arr != null)
			for (JSON v : arr) {
				context.define(Integer.toString(cc++), FutureInstructionFactory.value(v, SourceInfo.internal("arg")));
			}

		context.define("@", FutureInstructionFactory.value(arr, SourceInfo.internal("args")));
		context.define("_", FutureInstructionFactory.value(data, SourceInfo.internal("input")));

		ListenableFuture<JSON> j = inst.call(context, dd);
		// wait for results to resolve
		return j.get();
	}
}
