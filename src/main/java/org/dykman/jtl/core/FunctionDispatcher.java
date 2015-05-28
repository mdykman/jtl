package org.dykman.jtl.core;

import org.dykman.jtl.core.parser.JSONBuilder;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class FunctionDispatcher {
	public static ListenableFuture<JSON> executeFunction(String name, ListenableFuture<JSON> ctx,JSONBuilder builder) {
		return Futures.immediateFuture(builder.value("function " + name + "()"));
	}
}
