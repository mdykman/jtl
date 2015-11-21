package org.dykman.jtl.operator;

import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class RangeInstruction extends AbstractFutureInstruction {

	final FutureInstruction<JSON> f;
	final FutureInstruction<JSON> s;
	protected int size;
	public RangeInstruction(FutureInstruction<JSON> f,FutureInstruction<JSON> s, SourceInfo info) {
		super(info);
		this.f = f;
		this.s = s;
		this.size = 0;
	}
	
	public void setSize(int size) {
		this.size = size;
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
				JSON a = input.get(0);
				JSON b = input.get(1);
				if(a.isNumber() && b.isNumber()) {
					long al = ((JSONValue)a).longValue();

					long bl = ((JSONValue)b).longValue();
					if(size>0) {
						if(al<0) al = (al+size) % size;
						if(bl<0) bl = (bl+size) % size;
					}
					JSONArray out = context.builder().array(null);
					if(al<bl) {
						for(int i = 0; i + al <= bl; ++i) {
							long val = i+al;
							out.add(context.builder().value(i+al));
						}
					} else {
						for(int i = 0; al - i >= bl; ++i) {
							out.add(context.builder().value(al-i));
						}
					}
					return immediateCheckedFuture(out);
				}
				throw new ExecutionException("invalid range expression in array declaration",source);

				
			}
		});
	}

}
