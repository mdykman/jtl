package org.dykman.jtl;

@SuppressWarnings("serial")
public class ExecutionException extends Exception {

   SourceInfo info;
	public ExecutionException(String message, Throwable cause,SourceInfo info) {
		super(message, cause);
      this.info = info;
	}
	
	public ExecutionException(String message, java.util.concurrent.ExecutionException cause,SourceInfo info) {
		super(message, cause.getCause());
      this.info = info;
	}
	
	

	public ExecutionException(String message,SourceInfo info) {
		super(message);
		this.info = info;
	}
	
	public ExecutionException(java.util.concurrent.ExecutionException cause,SourceInfo info) {
		super(cause.getCause());
		this.info = info;
	}

	public ExecutionException(Throwable cause,SourceInfo info) {
		super(cause);
		this.info = info;
	}

	public ExecutionException(SourceInfo info) {
      this.info = info;
	}

	public SourceInfo getSourceInfo() {
	   return info;
	}
	
	public String report() {
	   StringBuilder builder = new StringBuilder();
	   builder.append("Exception: ").append(getLocalizedMessage())
	      .append(" -- ").append(info.name).append(" ")
	      .append(info.line).append(":").append(info.position);
	   return builder.toString();
	}
}
