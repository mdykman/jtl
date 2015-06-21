package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.core.Frame;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.AsyncFunction;

import static com.google.common.util.concurrent.Futures.*;

import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractInstructionFuture implements
		InstructionFuture<JSON> {

	@Override
	public final ListenableFuture<JSON> call(
			final AsyncExecutionContext<JSON> context,
			final ListenableFuture<JSON> data) throws ExecutionException {
		return transform(data, new AsyncFunction<JSON, JSON>() {

			@Override
			public ListenableFuture<JSON> apply(JSON input) {
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
										if (j.getType() != JSONType.NULL) {
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

	protected String stringValue(JSON j) {
		if (j == null)
			return null;
		switch (j.getType()) {
		case STRING:
		case DOUBLE:
		case LONG:
			return ((JSONValue) j).stringValue();
		default:
			return null;
		}
	}

	public abstract ListenableFuture<JSON> callItem(
			AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
			throws ExecutionException;

}
