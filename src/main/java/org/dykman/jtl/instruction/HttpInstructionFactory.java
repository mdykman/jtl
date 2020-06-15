package org.dykman.jtl.instruction;

import javax.servlet.http.HttpServletResponse;

import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONValue;
import org.dykman.jtl.operator.FutureInstruction;

public abstract class HttpInstructionFactory extends ContextualInstructionFactory {

	public static FutureInstruction<JSON> responseHeader() {
		SourceInfo info = SourceInfo.internal("ContextualInstructionFactory::responseHeader");
	
		ContextualInstructionFactory.PassiveObjectPairs rop = new ContextualInstructionFactory.PassiveObjectPairs() {
	
			@Override
			public void pair(AsyncExecutionContext<JSON> context, JSON f, JSON s) {
				HttpServletResponse response = context.getResponse();
				if (response == null) {
					System.err.println(String.format("http: %s:%s", f.stringValue(), s.stringValue()));
				} else
					response.addHeader(f.stringValue(), s.stringValue());
	
			}
		};
		return ContextualInstructionFactory.contextualPassthroughVarArgInstruction(info, rop);
	}

	public static FutureInstruction<JSON> httpStatus() {
		SourceInfo info = SourceInfo.internal("ContextualInstructionFactory::httpStatus");
	
		ContextualInstructionFactory.VoidNop<JSON> rop = new ContextualInstructionFactory.VoidNop<JSON>() {
	
			@Override
			public void apply(AsyncExecutionContext<JSON> context, JSON... vargs) {
				if (vargs.length == 0) {
					System.err.println(String.format("status requires 1 argument"));
				} else {
					HttpServletResponse response = context.getResponse();
					if (response == null) {
						System.err.println(String.format("error retrieving response object"));
					} else {
						JSON j = vargs[0];
						if (j.isValue()) {
							JSONValue jv = (JSONValue) j;
							Long s = jv.longValue();
							if (s == null) {
								System.err.println(
										String.format("invalid value specified for status: %s", jv.stringValue()));
							} else if (vargs.length == 1) {
								int si = s == null ? 0 : s.intValue();
								response.setStatus(si);
							} else if (vargs.length >= 2) {
								// deprecated before it's implemented... oh,well, someone will watt this
								System.err.println(String.format("setting http status text is deprecated"));
								response.setStatus(jv.longValue().intValue(), vargs[1].stringValue());
							}
						}
						;
					}
				}
	
			}
		};
		return ContextualInstructionFactory.contextualPassthroughVarArgInstruction(info, rop);
	
	}

}
