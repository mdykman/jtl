package org.dykman.jtl.future;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.immediateFailedCheckedFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.json.Frame;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class FramingInstructionFuture extends AbstractInstructionFuture {

	public FramingInstructionFuture() {
		// TODO Auto-generated constructor stub
	}

	public abstract ListenableFuture<JSON> callItem(AsyncExecutionContext<JSON> context,
			ListenableFuture<JSON> data) throws ExecutionException;
	
	
	@Override
	public ListenableFuture<JSON> call(
			final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data) throws ExecutionException {
		return transform(data, new AsyncFunction<JSON, JSON>() {

			@Override
			public ListenableFuture<JSON> apply(JSON input) {
//				final Frame outputFrame = context.builder().frame();
				if (input.getType() == JSONType.FRAME) {
					Frame frame = (Frame) input;
					List<ListenableFuture<JSON>> rr = new ArrayList<>();
					for (JSON j : frame) {
						try {
							ListenableFuture<JSON> r = callItem(context,
									immediateCheckedFuture(j));
							rr.add(callItem(context, immediateCheckedFuture(j)));
						} catch (ExecutionException e) {
							rr.add(immediateFailedCheckedFuture(e));
						}
					}
					return transform(allAsList(rr),
							new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(
										List<JSON> _input) {
									Frame newf = context.builder().frame();
									for (JSON j : _input) {
										if (j.getType() != JSONType.NULL && j.isTrue()) {
											newf.add(j);
										}
									}
									return immediateCheckedFuture(newf);
								}
							});
				} else {
					try {
						return callItem(context, immediateCheckedFuture(input));
					} catch (ExecutionException e) {
						return immediateFailedCheckedFuture(e);
					}
				}
			}
		});
	}


}
