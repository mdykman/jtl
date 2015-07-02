//import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
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

public class SimpleJtlTest {


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			JSONBuilder builder = new JSONBuilderImpl();
			JtlCompiler compiler = new JtlCompiler(builder,true,false,false);
			
			System.err.println("compiling " + args[0]);
			
			FileInputStream fin = new FileInputStream(args[0]);
			InstructionFuture<JSON> inst = compiler.parse(fin);
			if(inst == null) {
				System.err.println("no program");
				System.exit(1);
			}
			
			JSON data = builder.parse(new File(args[1]));
			JSONObject config = (JSONObject)builder.parse(new File(args[2]));
	
			InstructionFutureFactory factory = new InstructionFutureFactory(builder);
			ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
			AsyncExecutionContext<JSON>  context = JtlCompiler.createInitialContext(data,config, factory, les);
			
			ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
			PrintWriter pw =new PrintWriter(System.out);
			j.get().write(pw, 3,false);
			pw.flush();
			les.shutdownNow();
			les.awaitTermination(2, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
