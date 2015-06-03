package org.dykman.jtl.core.parser;

import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.Pair;

import com.google.common.util.concurrent.ListenableFuture;

public class JtlValue {
	JSON                                jval;
	String                              sval;
	Pair<String, JSON>                   dval;
	ListenableFuture<JSON>              jfval;
	ListenableFuture<String>            sfval;
	ListenableFuture<Pair<String, JSON>> dfval;
}
