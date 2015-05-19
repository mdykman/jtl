package org.dykman.jtl.core.engine;

public class ExecutionException extends Exception {

	public ExecutionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
	public ExecutionException(String message, java.util.concurrent.ExecutionException cause) {
		super(message, cause.getCause());
	}

	public ExecutionException(String message) {
		super(message);
	}
	public ExecutionException(java.util.concurrent.ExecutionException cause) {
		super(cause.getCause());
	}

	public ExecutionException(Throwable cause) {
		super(cause);
	}

	public ExecutionException() {
	}

}
