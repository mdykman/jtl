package org.dykman.jtl.ext;

import java.util.List;

import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;

public abstract class Prototype {

	public Prototype() {
		// TODO Auto-generated constructor stub
	}
	
	public abstract JSON invoke(AsyncExecutionContext<JSON> context,JSON data, List<JSON> params);

}
