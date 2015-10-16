package org.dykman.jtl.future;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class RangeInstruction extends AbstractFutureInstruction {

	FutureInstruction<JSON> f;
	FutureInstruction<JSON> s;
	public RangeInstruction(FutureInstruction<JSON> f,FutureInstruction<JSON> s, SourceInfo info) {
		super(info);
		this.f = f;
		this.s = s;
	}
	@Override
	public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException {
		List<ListenableFuture<JSON>> ll = new ArrayList<>();
		ll.add(f.call(context, data));
		ll.add(s.call(context, data));
		return Futures.transform(Futures.allAsList(ll),new AsyncFunction<List<JSON>, JSON>() {

			@Override
			public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
				Iterator<JSON> it = input.iterator();
				JSONArray arr = context.builder().array(null);
				arr.setRanged(true);
				arr.add(it.next());
				arr.add(it.next());
				return Futures.immediateCheckedFuture(arr);
			}
		});
	}

}
