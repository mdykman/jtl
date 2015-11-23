package org.dykman.jtl.test;

import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONBuilderImpl;

public class A {

	public static void main(String[] args) {
		
		String s = "one/two";
		JSONBuilder builder = new JSONBuilderImpl();
		JSON j = builder.value(s);
		System.out.println(j.toString());
		System.out.println(j.stringValue());
		// TODO Auto-generated method stub

	}

}
