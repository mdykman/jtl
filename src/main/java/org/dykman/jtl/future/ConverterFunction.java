package org.dykman.jtl.future;

import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;

import com.google.common.util.concurrent.AsyncFunction;

abstract public class ConverterFunction implements AsyncFunction<JSON, JSON> {
	JSONBuilder builder;

	public void setBuilder(JSONBuilder b) {
		builder = b;
	}
}