//import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
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

      ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		try {
			JSONBuilder builder = new JSONBuilderImpl();
			JtlCompiler compiler = new JtlCompiler(builder,false,false,false);
			
			System.err.println("compiling " + args[0]);
			File inputFile = new File(args[0]);
			FileInputStream fin = new FileInputStream(inputFile);
			InstructionFuture<JSON> inst = compiler.parse(args[0],fin);
			if(inst == null) {
				System.err.println("no program");
				System.exit(1);
			}
			
			JSON data = builder.parse(new File(args[1]));
			JSONObject config = (JSONObject)builder.parse(new File(args[2]));
	
			AsyncExecutionContext<JSON>  context = JtlCompiler.createInitialContext(data,config, 
				inputFile.getParentFile(),builder, les);
//			context.debug(true);
			ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
			PrintWriter pw =new PrintWriter(System.out);
			j.get().write(pw, 3,false);
			pw.flush();
		} catch (Exception e) {
			e.printStackTrace();
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
