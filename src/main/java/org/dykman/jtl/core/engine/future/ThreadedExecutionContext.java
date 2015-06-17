package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.immediateFailedCheckedFuture;

import java.util.concurrent.Callable;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class ThreadedExecutionContext extends SimpleExecutionContext {

	final ListeningExecutorService executorService;

	public ThreadedExecutionContext(
			ListeningExecutorService executorService,
			AsyncExecutionContext<JSON> parent,
			boolean fc) {
		super(parent, fc);
		this.executorService = executorService;
	}

	@Override
	public ListenableFuture<JSON> execute(final InstructionFuture<JSON> inst,
			final ListenableFuture<JSON> data) {
			final SimpleExecutionContext ctx = this;
			Callable<JSON> cc = new Callable<JSON>() {
				@Override
				public JSON call() throws Exception {
//					try {
					return inst.call(ctx, data).get();
//					} catch (ExecutionException e) {
//						return immediateFailedCheckedFuture(e);
//					}
				}
			};
			return executorService.submit(cc);
//			executorService.execute(cc);
	}
}
