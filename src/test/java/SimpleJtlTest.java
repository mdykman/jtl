//import org.antlr.v4.runtime.CommonTokenStream;

import java.io.FileInputStream;

import main.antlr.jtlLexer;
import main.antlr.jtlParser;
import main.antlr.jtlParser.JtlContext;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONBuilderImpl;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JtlCompiler;
import org.dykman.jtl.core.engine.future.InstructionFuture;
import org.dykman.jtl.core.engine.future.InstructionFutureVisitor;

public class SimpleJtlTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			JSONBuilder builder = new JSONBuilderImpl();
			JtlCompiler compiler = new JtlCompiler(builder);
			FileInputStream fin = new FileInputStream(args[0]);
			InstructionFuture<JSON> inst =compiler.parse(fin);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
