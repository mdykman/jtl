package org.dykman.jtl.future;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class FunctionInstruction extends AbstractInstructionFuture {
		final String name;
		final List<InstructionFuture<JSON>> iargs;
		boolean variable = false;

		public FunctionInstruction(SourceInfo info, String name, List<InstructionFuture<JSON>> iargs) {
			super(info);
			this.name = name;
			this.iargs = iargs;
		}

		public void setVariable(boolean b) {
			variable = b;
		}

		protected AsyncExecutionContext<JSON> setupArguments(AsyncExecutionContext<JSON> dctx, final String name,
				final List<InstructionFuture<JSON>> iargs, final ListenableFuture<JSON> data) {
			AsyncExecutionContext<JSON> context = dctx.createChild(true, false, data, source);
			List<InstructionFuture<JSON>> insts = new ArrayList<>();
			context.define("0", InstructionFutureFactory.value(context.builder().value(name), source));
			int cc = 1;
			// Iterator<InstructionFuture<JSON>> iit = iargs.iterator();
			if (iargs != null)
				for (InstructionFuture<JSON> inst : iargs) {
					String key = Integer.toString(cc++);
					context.define(key, InstructionFutureFactory.memo(source, InstructionFutureFactory.deferred(source, inst, dctx.declaringContext(), data)));
					insts.add(inst);
				}
			
//			AsyncExecutionContext<JSON> declaring = dctx.declaringContext();
			boolean subst = (!variable) ; // && (dctx != declaring) && dctx.isFunctionContext();
			if (subst) {
				int ss = 1;
				while (true) {
					String skey = Integer.toString(ss++);
					InstructionFuture<JSON> si = dctx.getdef(skey);
					if (si == null)
						break;
					String key = Integer.toString(cc++);
					// context.define(key, new DeferredCall(source, si, ctx,
					// data));
					context.define(key, si);
					insts.add(si);
				}

			}
			context.define("@", InstructionFutureFactory.paramArray(source, insts));
//			context.define("#", value(context.builder().value(insts.size()), source));
			/*
			 * while(true) { String key = Integer.toString(cc++);
			 * InstructionFuture<JSON> inst = iit.hasNext() ? iit.next() : null;
			 * if(inst == null &&) if(subst) { InstructionFuture<JSON> si =
			 * ctx.getdef(key); if(si!=null) context.define(key, si); else
			 * if(inst != null) context.define(key, inst); else break; } else {
			 * if(inst == null) break; context.define(key, inst); }
			 * 
			 * }
			 */
			/*
			 * for(InstructionFuture<JSON> i : iargs) { // the arguments
			 * themselves should be evaluated // with the parent context //
			 * instructions can be unwrapped if the callee wants a // a
			 * function, rather than a value from the arument list String key =
			 * Integer.toString(cc++);
			 * 
			 * InstructionFuture<JSON> inst = ctx.getdef(key); if(inst == null)
			 * inst = deferred(meta, i, ctx.declaringContext(), data); // but
			 * define the argument in the child context
			 * 
			 * // this strategy allows numbered argument (ie.) $1 to be used
			 * context.define(key, inst); }
			 */
			return context;
		}

		@Override
		public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
				final ListenableFuture<JSON> data) throws ExecutionException {
			String[] ss = name.split("[.]", 2);
			InstructionFuture<JSON> func;
			if (ss.length == 1) {
				func = context.getdef(name);
			} else {
				AsyncExecutionContext<JSON> ctx = context.getNamedContext(ss[0]);
				if (ctx == null) {
					throw new ExecutionException("unable to load named context " + ss[0], source);
				}
				func = ctx.getdef(ss[1]);
			}
			if (func == null) {
				throw new ExecutionException("no function found named " + name, source);
			}
			AsyncExecutionContext<JSON> childContext = setupArguments(context, name, iargs, data);
			return func.call(childContext, data);
		}
	}