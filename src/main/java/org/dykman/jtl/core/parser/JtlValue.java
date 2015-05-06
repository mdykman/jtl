package org.dykman.jtl.core.parser;

import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.JSON;

import com.google.common.util.concurrent.ListenableFuture;

public class JtlValue {
	JSON                                jval;
	String                              sval;
	Duo<String, JSON>                   dval;
	ListenableFuture<JSON>              jfval;
	ListenableFuture<String>            sfval;
	ListenableFuture<Duo<String, JSON>> dfval;
}
