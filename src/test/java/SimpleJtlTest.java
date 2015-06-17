//import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONBuilderImpl;
import org.dykman.jtl.core.JtlCompiler;
import org.dykman.jtl.core.engine.future.InstructionFuture;
import org.dykman.jtl.core.engine.future.SimpleExecutionContext;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class SimpleJtlTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			JSONBuilder builder = new JSONBuilderImpl();
			JtlCompiler compiler = new JtlCompiler(builder);
			FileInputStream fin = new FileInputStream(args[0]);
			InstructionFuture<JSON> inst = compiler.parse(fin);
			System.err.println("made it through the compiler");
			if(inst == null) {
				System.err.println("no program");
				System.exit(1);
			} else {
				System.err.println("obtained " + inst.getClass().getName());
			}
			JSON data = builder.parse(new File(args[1]));
			System.err.println("acquired data");
			SimpleExecutionContext context = new SimpleExecutionContext();
			ListenableFuture<JSON> j = inst.call(context, Futures.immediateFuture(data));
			PrintWriter pw =new PrintWriter(System.out);
			j.get().write(pw, 3);
			pw.flush();
//			System.out.println(j.get().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
