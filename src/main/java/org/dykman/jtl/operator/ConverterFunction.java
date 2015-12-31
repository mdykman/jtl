package org.dykman.jtl.operator;

import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;

import com.google.common.util.concurrent.AsyncFunction;

abstract public class ConverterFunction implements AsyncFunction<JSON, JSON> {
	public JSONBuilder builder;

	public void setBuilder(JSONBuilder b) {
		builder = b;
	}
}