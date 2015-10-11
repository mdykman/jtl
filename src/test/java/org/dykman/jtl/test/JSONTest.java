package org.dykman.jtl.test;

import org.dykman.jtl.json.*;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class JSONTest {

	File base = new File("src/test/resources");
	
	JSONBuilder builder = new JSONBuilderImpl();

	@Test
	public void testOverlay() 
	throws Exception {
		JSONObject o1 = (JSONObject)builder.parse(new File(base,"test-overlay1.json"));		
		JSONObject o2 = (JSONObject)builder.parse(new File(base,"test-overlay2.json"));	
		JSONObject o3 = (JSONObject)builder.parse(new File(base,"test-overlay3.json"));
		JSONObject o4 = o1.overlay(o2);
		assertEquals(o4, o3);
	}
	
	@Test
	public void testEquals() 
	throws Exception {
		JSONObject o1 = (JSONObject)builder.parse(new File(base,"test-overlay3.json"));
		JSONObject o2 = (JSONObject)builder.parse(new File(base,"test-overlay3.json"));
		assertTrue(o1.equals(o2));
	}
		
	@Test
	public void testClone() 
	throws Exception {
		JSONObject o1 = (JSONObject)builder.parse(new File(base,"test-overlay3.json"));
		JSONObject o2 = (JSONObject)o1.cloneJSON();
		assertEquals(o2, o1);
	}
	
	@Test
	public void testArraySize() 
	throws Exception {
		JSONArray array = builder.array(null);
		array.add(builder.value(1));
		array.add(builder.value(1));
		array.add(builder.value(1));
		array.add(builder.value(1));
		assertTrue(array.size() == 4);
		
		array.add(builder.value("str"));
		array.add(builder.value("str"));
		array.add(builder.value("str"));
		array.add(builder.value("str"));
		assertTrue(array.size() == 8);

	
		array.add(builder.array(null));
		array.add(builder.array(null));
		array.add(builder.array(null));
		array.add(builder.array(null));
		assertTrue(array.size() == 12);

	}
}
