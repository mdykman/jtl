//import org.antlr.v4.runtime.CommonTokenStream;

import main.antlr.jtlLexer;
import main.antlr.jtlParser;
import main.antlr.jtlParser.JtlContext;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.dykman.jtl.core.parser.InstructionFutureVisitor;

public class SimpleJtlTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			jtlLexer lexer = new jtlLexer(new ANTLRFileStream(args[0]));
			jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
			JtlContext tree = parser.jtl();
			InstructionFutureVisitor visitor = new InstructionFutureVisitor();
			visitor.visit(tree);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
