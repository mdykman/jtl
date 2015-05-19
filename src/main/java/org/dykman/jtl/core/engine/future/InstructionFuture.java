package org.dykman.jtl.core.engine.future;

import java.util.List;

import org.dykman.jtl.core.JSONException;

import com.google.common.util.concurrent.ListenableFuture;

public interface InstructionFuture<T> {

	public ListenableFuture<T> call(AsyncExecutionContext<T> context, ListenableFuture<T> data)
		throws JSONException;
//	public ListenableFuture<List<T>> callChildren(Engine<T> eng,ListenableFuture<T> t);
}
