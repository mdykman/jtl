package org.dykman.jtl.instruction;

import java.util.IllegalFormatConversionException;

import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilderImpl;
import org.dykman.jtl.json.JSONValue;
import org.dykman.jtl.operator.FutureInstruction;

public abstract class TextInstructionFactory extends ContextualInstructionFactory {

	public static FutureInstruction<JSON> sprintf() {
		SourceInfo info = SourceInfo.internal("TextInstructionFactory::sprintf");
		return ContextualInstructionFactory.contextualVarArgInstruction(info, (context, args) -> {
			int length = args.length;
			if (length > 0) {
				String format = args[0].stringValue();
				Object[] objs = new Object[length - 1];
				for (int i = 1; i < length; ++i) {
					JSON j = args[i];
					if(j == null) j = JSONBuilderImpl.NULL;
					switch (j.getType()) {
					case BOOLEAN:
						objs[i - 1] = ((JSONValue) j).booleanValue();
						break;
					case LONG:
						objs[i - 1] = ((JSONValue) j).longValue();
						break;
					case DOUBLE:
						objs[i - 1] = ((JSONValue) j).doubleValue();
						break;
					case STRING:
						objs[i - 1] = ((JSONValue) j).stringValue();
						break;
					case OBJECT:
					case ARRAY:
					case LIST:
						objs[i - 1] = "(illegal value)";
						break;
					case NULL:
						objs[i - 1] = "(null)";
						break;
					}
				}
				try {
					return context.builder().value(String.format(format, objs));
				} catch(IllegalFormatConversionException e) {
					StringBuilder sb = new StringBuilder();
					sb.append("sprintf error: ").append(e.getLocalizedMessage()).append(System.lineSeparator());
					sb.append(" format: \"").append(format).append("\", [ ");
					for (int i = 1; i < length; ++i) {
						if(i > 1) sb.append(", ");
						sb.append(args[i].getClass().getName());
					}
					sb.append("]");
					System.err.println(sb.toString());
					context.builder().value(sb.append(System.lineSeparator()).toString());
				}
	
			}
			// if called neither format string nor args, what else are you going to do?
			System.err.println("sprintf invoked with  no format string or arguments");
			return context.builder().value("");
		});
	
	}

}
