package org.dykman.jtl.core.engine;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;

public interface InstructionFuture<T> {

	public ListenableFuture<T> call(AsyncEngine<T> eng,ListenableFuture<T> parent);
//	public ListenableFuture<List<T>> callChildren(Engine<T> eng,ListenableFuture<T> t);
}
