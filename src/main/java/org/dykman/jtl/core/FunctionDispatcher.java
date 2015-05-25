package org.dykman.jtl.core;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class FunctionDispatcher {
	public static ListenableFuture<JSON> executeFunction(String name, ListenableFuture<JSON> ctx) {
		return Futures.immediateFuture(new JSONValue(null,"function " + name + "()"));
	}
}
