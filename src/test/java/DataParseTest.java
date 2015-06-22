import java.io.FileInputStream;
import java.io.PrintWriter;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONBuilderImpl;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.engine.JSONFactory;

public class DataParseTest {


	public DataParseTest(){
		
	}/*
	public JSON parse(InputStream in) 
		throws IOException {
		jtlLexer lexer = new jtlLexer(new ANTLRInputStream(in));
		jtlParser parser = new jtlParser(new CommonTokenStream(lexer));
		JtlContext tree = parser.jtl();
		DataVisitor visitor = new DataVisitor(new JSONBuilder());
		DataValue<JSON> v = visitor.visit(tree);
		return v.value;
		
	}*/
	public static void main(String[] args) {
		try {
			JSONBuilder builder = new JSONBuilderImpl();
			JSON json = builder.parse(new FileInputStream(args[0]));
			if(json == null) {
				System.err.println("parser returned null");
			} else {
			PrintWriter pw = new PrintWriter(System.out);
			json.write(pw, 1,false);
			pw.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
