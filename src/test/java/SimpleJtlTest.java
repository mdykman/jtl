//import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONBuilderImpl;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JtlCompiler;
import org.dykman.jtl.core.engine.future.AsyncExecutionContext;
import org.dykman.jtl.core.engine.future.InstructionFuture;
import org.dykman.jtl.core.engine.future.InstructionFutureFactory;
import org.dykman.jtl.core.engine.future.SimpleExecutionContext;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class SimpleJtlTest {


	public static AsyncExecutionContext<JSON> createInitialContext(
			JSONObject config,
			InstructionFutureFactory factory,
			ListeningExecutorService les ) {
		
		SimpleExecutionContext context = new SimpleExecutionContext(factory.builder(),config);
		JSONObject modules= (JSONObject)config.get("modules");
		context.define("module", factory.loadModule(modules));
		context.define("error", factory.defaultError());
		context.setExecutionService(les);
		return context;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			JSONBuilder builder = new JSONBuilderImpl();
			JtlCompiler compiler = new JtlCompiler(builder,false,false);
			
			System.err.println("compiling " + args[0]);
			
			FileInputStream fin = new FileInputStream(args[0]);
			InstructionFuture<JSON> inst = compiler.parse(fin);
			if(inst == null) {
				System.err.println("no program");
				System.exit(1);
			}
			JSON data = builder.parse(new File(args[1]));
			System.err.println("acquired data");
			JSONObject config = (JSONObject)builder.parse(new File(args[2]));
	
			InstructionFutureFactory factory = new InstructionFutureFactory(builder);
			ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
			AsyncExecutionContext<JSON>  context = createInitialContext(config, factory, les);
			
			ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
			PrintWriter pw =new PrintWriter(System.out);
			j.get().write(pw, 3);
			pw.flush();
			les.shutdownNow();
			les.awaitTermination(2, TimeUnit.SECONDS);
//			System.out.println(j.get().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
