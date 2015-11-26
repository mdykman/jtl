package org.dykman.jtl.operator;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class DataAwareInstruction extends AbstractFutureInstruction {

	final boolean blind;
	final int minargs;
	AsyncFunction<List<JSON>, JSON> func;

	public DataAwareInstruction(SourceInfo meta, boolean items,
			boolean blind, int minargs,AsyncFunction<List<JSON>, JSON> func) {
		super(meta, items);
		this.func = func;
		this.blind = blind;
		this.minargs = minargs;
	}

	@Override
	public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException {
		List<ListenableFuture<JSON>> ll = new ArrayList<>();
		if(!blind) ll.add(data);
		int cc = 0;
		while(true) {
			FutureInstruction<JSON> inst = context.getdef(Integer.toString(++cc));
			if(inst==null) break;
			ll.add(inst.call(context, data));
		}
		if(cc<minargs) throw new ExecutionException("execting minimum " + minargs 
				+ " arguments, " + cc + " provided",source);
		return Futures.transform(Futures.allAsList(ll), func);
	}

}
