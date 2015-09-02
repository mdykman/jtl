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

@SuppressWarnings("deprecation")
public class JtlMain {

	JSONBuilder builder = new JSONBuilderImpl();
	InstructionFutureFactory factory = new InstructionFutureFactory();
	JtlCompiler compiler = new JtlCompiler(builder, false, false, false);
	ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors
			.newCachedThreadPool());

	JSON config = builder.value();

	public static void printHelp(Options cl) {
		System.out.println(
//				" $ java " + JtlMain.class.getName()
				" $ jtl " 
				+ " [options ...] arg1 arg2 ...");
		System.out.println();
		System.out.println("  JTL is a language, library and tool for parsing, creating and transforming JSON data");
		System.out
				.println("  see: https://github.com/mdykman/jtl");
		System.out.println();

		Iterator<Option> oit = cl.getOptions().iterator();
		while (oit.hasNext()) {
			Option oo = oit.next();
			StringBuilder builder = new StringBuilder();
			builder.append("  -").append(oo.getOpt()).append(" --")
					.append(oo.getLongOpt()).append("\t")
					.append(oo.getDescription());
			System.out.println(builder.toString());
		}
		System.out.println();

		System.out.println("  examples:");
		System.out.println("    $ jtl src/test/resources/group.jtl src/test/resources/generated.json");
		System.out.println("    $ jtl src/test/resources/variables.jtl src/test/resources/data.json");
		System.out.println();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JtlMain main = new JtlMain();
		try {
			String s = System.getProperty("jtl.home");
			File home = s == null ? new File("."): new File(s);

			Options options = new Options();
			options.addOption(new Option("c", "config", true,
					"specify a configuration file"));
			options.addOption(new Option("x", "jtl", true,
					"specify a jtl file"));
			options.addOption(new Option("p", "port", true,
					"specify a port number (default:7718) * implies --server"));
			options.addOption(new Option("d", "data", true,
					"specify input data (json file)"));
			options.addOption(new Option("h", "help", false,
					"print help message"));
			options.addOption(new Option("D", "directory", true,
					"specify base directory"));
			options.addOption(new Option("s", "server", false,
					"run in server mode (default port:7718"));
			options.addOption(new Option("D", "directory", true,
					"specify base directory (default:."));
			options.addOption(new Option("n", "indent", true,
					"specify default indent level for output (default:3)"));
			options.addOption(new Option("q", "quote", false,
					"enforce quoting of all object keys (default:false)"));
			options.addOption(new Option("a", "array", false,
					"parse a sequence of json entities from the input stream and "
							+ "assemble them into an array"));

			int c;
			boolean help = false;
			File fconfig = null;
			File jtl = null;
			File fdata = null;
			int indent = 3;
			boolean dirSet = false;
			boolean array = false;
			boolean enquote = false;
			File cexddir = new File(".");
//			File cdordir = new File(".");
			boolean serverMode = false;
			int port = 7719; // default port

			CommandLineParser parser = new GnuParser();
			CommandLine cli = parser.parse(options, args);

			String oo;
			if (cli.hasOption('D') || cli.hasOption("directory")) {
				oo = cli.getOptionValue('D');
				if (oo == null)
					oo = cli.getOptionValue("directory");
				cexddir = new File(oo);
				dirSet = true;
			}
			if (cli.hasOption('a') || cli.hasOption("array")) {
				array = true;
			}
			if (cli.hasOption('c') || cli.hasOption("config")) {
				oo = cli.getOptionValue('c');
				if (oo == null)
					oo = cli.getOptionValue("config");

				if (dirSet)
					fconfig = new File(cexddir, oo);
				else
					fconfig = new File(oo);
			}
			if (cli.hasOption('q') || cli.hasOption("quote")) {
				enquote = true;
			}
			if (cli.hasOption('x') || cli.hasOption("jtl")) {
				oo = cli.getOptionValue('c');
				oo = cli.getOptionValue("jtl");
				jtl = new File(oo);
				if (!dirSet) {
					cexddir = jtl.getParentFile();
				}

			}
			if (cli.hasOption('s') || cli.hasOption("server")) {
				serverMode = true;
			}
			if (cli.hasOption('h') || cli.hasOption("help")) {
				printHelp(options);
				System.exit(0);
			}
			if (cli.hasOption('d') || cli.hasOption("data")) {
				oo = cli.getOptionValue('d');
				if (oo == null)
					oo = cli.getOptionValue("data");
				if (dirSet)
					fdata = new File(cexddir, oo);
				else
					fdata = new File(oo);
			}
			if (cli.hasOption('i') || cli.hasOption("indent")) {
				oo = cli.getOptionValue('i');
				if (oo == null)
					oo = cli.getOptionValue("indent");
				indent = Integer.parseInt(oo);
			}

			if (help) {
				printHelp(options);
				System.exit(0);
			}

			if (fconfig != null)
				main.setConfig(fconfig);

			if (serverMode) {
				JtlServer server = main.launchServer(jtl, port);
				server.start();
				server.join();
			} else {
				List<String> argList = cli.getArgList();
				Iterator<String> argIt = argList.iterator();
				int offset = 0;
				if (jtl == null) {
					// if not specified with a switch, the script must be the
					// first
					// name on the argument list
					String[] aa = cli.getArgs();
					if (argList.size() == 0) {
						throw new RuntimeException(
								"could not determine input script");
					}
					// System.err.println("running script " + aa[0]);
					jtl = new File(argIt.next());
					offset = 1;
				}
				InstructionFuture<JSON> inst = main.compile(jtl);
				PrintWriter pw = new PrintWriter(System.out);
				if (array) {
					JSONArray arr = main.builder.array(null);
					while (true) {
						InputStreamReader reader = new InputStreamReader(
								System.in);
						try {
							JSON j = main.parse(System.in);
							System.out
									.println("============================================!!!!!!!!!!!!!!==============");
							arr.add(j);

						} catch (IOException e) {
							System.err
									.println("I hope THIS is antlr signaling EOF");
							e.printStackTrace();
							break;
						} catch (IllegalStateException e) {
							System.err
									.println("I hope this is antlr signaling EOF");
							e.printStackTrace();

							break;
						}
					}
					JSON result = main.execute(inst, arr, cexddir, fconfig);
					result.write(pw, indent, enquote);
					pw.flush();
				} else if (fdata != null) {
					JSON data = main.parse(fdata);
					JSON result = main.execute(inst, data, cexddir, fconfig);
					result.write(pw, indent, enquote);
//					pw.flush();

				} else {
					while (argIt.hasNext()) {
						File f = new File(argIt.next());
						JSON data = main.parse(f);
						JSON result = main
								.execute(inst, data, cexddir, fconfig);
						result.write(pw, indent, enquote);
					}
				}
				pw.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				main.shutdown();
			} catch (InterruptedException e) {
				System.err.println("error un shutdown: "
						+ e.getLocalizedMessage());
				// e.printStackTrace();
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
