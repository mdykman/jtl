package org.dykman.jtl;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.future.SimpleExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class JtlMain {

	JSONBuilder builder = new JSONBuilderImpl();
	InstructionFutureFactory factory = new InstructionFutureFactory();
	JtlCompiler compiler = new JtlCompiler(builder,false,false,false);
	ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

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
			
			StringBuffer sb = new StringBuffer();
			LongOpt[] opts = new LongOpt[3];
			opts[0] = new LongOpt("config", LongOpt.REQUIRED_ARGUMENT, sb, 'c');
			opts[1] = new LongOpt("jtl", LongOpt.REQUIRED_ARGUMENT, sb, 'x');
			opts[2] = new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, sb, 'p');
			opts[3] = new LongOpt("data", LongOpt.REQUIRED_ARGUMENT, sb, 'd');
			opts[4] = new LongOpt("help", LongOpt.REQUIRED_ARGUMENT, sb, 'h');
			opts[5] = new LongOpt("directory", LongOpt.REQUIRED_ARGUMENT, sb, 'D');
			
			int c;
			boolean help = false;
			File fconfig = null;
			File jtl = null;
			File fdata = null;
			File cwd = new File(".");
			boolean server = false;
			int port = 7719; // default port
			
			Getopt g = new Getopt("jtl", args,"h" ,opts);
			while((c = g.getopt()) != -1) {
				switch(c) {
					case 0: {
						fconfig = new File(home,g.getOptarg());
					} break;
					case 1: {
						jtl = new File(home,g.getOptarg());
					} break;
					case 2: {
						port = Integer.parseInt(g.getOptarg());
					}
					case 3: {
						fdata = new File(home,g.getOptarg());
					}
					case 4: {
						help = true;
					}
					case 5: {
						cwd = new File(home,g.getOptarg());
					}
					break;
				}
				
			}
			
			if(help) {
				printHelp();
				System.exit(0);
			}
			JtlMain main = new JtlMain();
			
			InstructionFuture<JSON> inst = main.compile(jtl);
			PrintWriter pw =new PrintWriter(System.out);
			if(fdata!= null) {
				JSON data = main.parse(fdata);
				BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
				String line;
				while((line=buffer.readLine())!=null) {
					main.compile(line);
					JSON result = main.execute(inst, data, cwd,fconfig);
					result.write(pw, 3,false);
					pw.flush();
				}
			} else {
				 for (int i = g.getOptind(); i < args.length ; i++) {
						File f = new File(args[i]);
						JSON data = main.parse(f);
						JSON result = main.execute(inst, data, cwd,fconfig);
						result.write(pw, 3,false);
						pw.flush();					 
				 }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void shutdown() throws InterruptedException { 
		les.shutdownNow();
		les.awaitTermination(2, TimeUnit.SECONDS);
	}	
	public InstructionFuture<JSON> compile(File f) 
		throws IOException {
		return compiler.parse(f);
	}
	public InstructionFuture<JSON> compile(String f) 
		throws IOException {
		return compiler.parse(f);
	}
	public JSON parse(File f) 
		throws IOException {
		return builder.parse(f);
	}
	
	public JSON execute(InstructionFuture<JSON> inst, JSON data,File cwd,File config) 
		throws Exception {
//		JSON d = builder.parse(data);
		JSON c; 
		if(config!=null && config.exists()) {
			c = builder.parse(config);
		} else {
			c = builder.value();
		}
		AsyncExecutionContext<JSON>  context = JtlCompiler.createInitialContext(data,c, cwd,builder, les);
//		InstructionFuture<JSON> inst = compiler.parse(jtl);
		ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
		return j.get();
	}
}
