package org.dykman.jtl.test;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.FutureInstruction;
import org.dykman.jtl.json.*;
import org.dykman.jtl.modules.ModuleLoader;

import static org.junit.Assert.*;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class JTLTest {

	File base = new File("src/test/resources");

	JSONBuilder builder;
	JtlCompiler compiler;
	JSONObject conf;
	ListeningExecutorService les;

	{
		ConsoleAppender console = new ConsoleAppender();
		String PATTERN = "%d [%p|%c|%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN));
		console.setThreshold(Level.toLevel(Level.ERROR_INT, Level.ERROR));
		console.activateOptions();
		org.apache.log4j.Logger.getRootLogger().addAppender(console);
	}

	@Rule
	public ExternalResource executor = new ExternalResource() {
		@Override
		protected void before() {
			builder = new JSONBuilderImpl();
			compiler = new JtlCompiler(builder);
			les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		}

		@Override
		protected void after() {
			try {
				builder = null;
				compiler = null;
				les.shutdown();
				les.awaitTermination(2000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				les.shutdownNow();
			}
		}
	};

	JSONObject getBaseData() throws Exception {
		return (JSONObject) builder.parse(new File(base, "samples.json"));
	}

	JSONObject getConfig() throws Exception {
		return (JSONObject) builder.parse(new File("conf/config.json"));
	}

	protected JSON runExpression(String expr, JSON data) throws Exception {
		FutureInstruction<JSON> inst = compiler.parse("test", expr);
		AsyncExecutionContext<JSON> context = compiler.createInitialContext(data, getConfig(),
				new File(".").getCanonicalFile(), null,builder, les);
		context.setInit(true);

		ModuleLoader ml = ModuleLoader.getInstance(context.currentDirectory(), context.builder(), getConfig());
		ml.loadAuto(context, true);

		ListenableFuture<JSON> result = inst.call(context, Futures.immediateCheckedFuture(data));
		JSON j = result.get();
		return j;
	}

	protected AsyncExecutionContext<JSON> setupContext(File code, JSON input, File init,JSONObject config) throws Exception {
		AsyncExecutionContext<JSON> context = compiler.createInitialContext(input, config,
				code.getCanonicalFile(), init,builder, les);
		config.put("server-mode", builder.value(false));

		context = context.createChild(false, false, 
				Futures.immediateCheckedFuture(input), SourceInfo.internal("test"));
		context.setRuntime(true);
		ModuleLoader ml = ModuleLoader.getInstance(context.currentDirectory(), context.builder(), config);
		ml.loadAuto(context, true);
		return context;
	}
	
	protected JSON runFile(File code, JSON input) throws Exception {
		return runFile(code, input, null, getConfig());
	}
	
	protected JSON runFile(File code, JSON input,File init,JSONObject config) throws Exception {
		FutureInstruction<JSON> inst = compiler.parse(code);
		ListenableFuture<JSON> inf = Futures.immediateCheckedFuture(input);
		AsyncExecutionContext<JSON> context = setupContext(code, input,init,config);

		ListenableFuture<JSON> result = inst.call(context, inf);
		JSON j = result.get();
		return j;
	}

	@Test
	public void testJdbc() throws Exception {
		JSON d = builder.parse(new File(base, "data2.json"));
		File code = new File(base, "test-jdbc.jtl");
		JSON res = runFile(code, d);
		System.out.println(res);
		JSON jj = runExpression(".", res);
//		assertEquals("g", jj.stringValue());
		JSON expected = builder.parse("['c','d','e','f','g']");
		jj=runExpression("*/children/*/*/children/*/*/name", res);
		assertEquals(expected, jj);
		jj=runExpression("**/filter(id==10)/name[0]", res);
//		expected = builder.parse("\"tracy rocks\"");
		assertEquals("tracy rocks", jj.stringValue());
//		 System.out.println(jj);
//		 jj = runExpression("**/filter(id = 3)", res);
//		 System.out.println(jj);
	}

	@Test
	public void testCount() throws Exception {
		JSON data = getBaseData();
		JSON j = runExpression("people/count()", data);
		assertTrue(j.isNumber());
		if (j.isNumber()) {
			assertTrue(((JSONValue) j).longValue() == 7);
		}
	}

	@Test
	public void testLabel() throws Exception {
		JSON data = getBaseData();
		JSON j = runExpression("people/element/unique()", data);
		assertTrue(j instanceof JSONArray);
		if (j instanceof JSONArray) {
			JSON expected = builder.parse("[\"fire\",\"ice\",\"candy\"]");
			assertEquals(j, expected);
		}
	}
	
	@Test public void testArrays() throws Exception {
		JSON j = runExpression("[20..29]", JSONBuilderImpl.NULL);
		assertTrue(j instanceof JSONArray);
		JSONArray array = (JSONArray) j;
		assertTrue(array.size() == 10);
	}
	@Test
	public void testDerefObject() throws Exception {
//		JSON expected = builder.parse("['c','d','e','f','g']");
		JSON data = builder.parse("{ foo:[2,4,6], bar: 'thing', fubar: true }");
		JSON j = runExpression(".['bar']", data);
		assertEquals("thing", j.stringValue());
		j = runExpression(".['foo','bar'][0]", data);
		assertTrue(j instanceof JSONArray);
		
		JSONArray array = (JSONArray) j;
		assertTrue(array.size() == 3);
	}
	@Test
	public void testDerefString() throws Exception {
		JSON data = builder.parse("\" abcdefghijklmnopqrstuvwxyz\"");
		JSON j = runExpression(".[8,5,12,12,15,0,23,15,18,12,4]", data);
		assertEquals("hello world", j.stringValue());
		j = runExpression(".[-4..-1]", data);
		assertEquals("wxyz", j.stringValue());

		j = runExpression(".[1..3]", data);
		assertEquals("abc", j.stringValue());
//		System.out.println(j.stringValue());
	}
	@Test
	public void testKeys() throws Exception {
		JSON data = getBaseData();
		JSON j = runExpression("people/keys()", data);
		assertTrue(j instanceof JSONArray);
		if (j instanceof JSONArray) {
			JSONArray arr = (JSONArray) j;
			assertTrue(arr.size() == 23);
		}
	}

	@Test
	public void testGroup() throws Exception {
		JSON data = getBaseData();
		JSON j = runExpression("people/group(element)/fire/count()", data);
		assertTrue(j.isNumber());
		if (j.isNumber()) {
			assertTrue(((JSONValue) j).longValue() == 2);
		}
	}

	@Test
	public void testOverlay() throws Exception {
		JSONObject o1 = (JSONObject) builder.parse(new File(base, "test-overlay1.json"));
		JSONObject o2 = (JSONObject) builder.parse(new File(base, "test-overlay2.json"));
		JSONObject o3 = (JSONObject) builder.parse(new File(base, "test-overlay3.json"));
		JSONObject o4 = o1.overlay(o2);
		assertEquals(o4, o3);
	}

	@Test
	public void testEquals() throws Exception {
		JSONObject o1 = (JSONObject) builder.parse(new File(base, "test-overlay3.json"));
		JSONObject o2 = (JSONObject) builder.parse(new File(base, "test-overlay3.json"));
		assertTrue(o1.equals(o2));
	}

	@Test
	public void testClone() throws Exception {
		JSONObject o1 = (JSONObject) builder.parse(new File(base, "test-overlay3.json"));
		JSONObject o2 = (JSONObject) o1.cloneJSON();
		assertEquals(o2, o1);
	}

	@Test
	public void testArraySize() throws Exception {
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
