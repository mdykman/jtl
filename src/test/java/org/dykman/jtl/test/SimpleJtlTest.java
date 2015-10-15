package org.dykman.jtl.test;
//import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.FutureInstruction;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONObject;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class SimpleJtlTest {


	public static void main(String[] args) {

	   long start = System.nanoTime();
      ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		try {
			JSONBuilder builder = new JSONBuilderImpl();
			JtlCompiler compiler = new JtlCompiler(builder);
			
			File inputFile = new File(args[0]);
			FileInputStream fin = new FileInputStream(inputFile);
         System.err.println("compiling " + args[0]);
	      long compile = System.nanoTime();
			FutureInstruction<JSON> inst = compiler.parse(args[0],fin);
			if(inst == null) {
				System.err.println("no program");
				System.exit(1);
			}
			
			JSON data = builder.parse(new File(args[1]));
			JSONObject config = (JSONObject)builder.parse(new File(args[2]));
	
			AsyncExecutionContext<JSON>  context = compiler.createInitialContext(data,config, 
				inputFile.getParentFile(),null,builder, les);
//			context.debug(true);
         long execute = System.nanoTime();
			ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
         long resolve = System.nanoTime();
			JSON jj = j.get();
			System.err.flush();
         long print = System.nanoTime();
         PrintWriter pw =new PrintWriter(System.out);
			jj.write(pw, 3,false);
			pw.flush();
			long done = System.nanoTime();
			
         System.out.println();
			System.out.println("total execution " + (done - start) + " ns");
			System.out.println("initializing " + (compile - start) + " ns");
			System.out.println("compiling " + (execute-compile) + " ns");
			System.out.println("executing " + (resolve-execute) + " ns");
			System.out.println("resovling " + (print-resolve) + " ns");
         System.out.println("printing " + (done -print) + " ns");
         
		} catch(ExecutionException e) {
		   System.err.println(e.getSourceInfo().toString(null));
		} catch (Exception e) {
		   if(e.getCause() instanceof ExecutionException ) {
	         System.err.println(e.getLocalizedMessage() + ": " + ((ExecutionException)e.getCause()).getSourceInfo().toString(null));
		   } else {
		      e.printStackTrace();
		   }
		} finally {
         les.shutdownNow();
         try {
            les.awaitTermination(2, TimeUnit.SECONDS);
         } catch (InterruptedException e) {
            // don't care, just exit
         }
		   
		}
	}
}
