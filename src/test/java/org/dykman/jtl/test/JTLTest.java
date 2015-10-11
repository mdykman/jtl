package org.dykman.jtl.test;

import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.*;

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

	JSONBuilder builder = new JSONBuilderImpl();
	JtlCompiler compiler = new JtlCompiler(builder);
	JSONObject conf;
	ListeningExecutorService les;

	@Rule
	public ExternalResource executor = new ExternalResource() {
		@Override
		protected void before() {
			les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		}

		@Override
		protected void after() {
			try {
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
		InstructionFuture<JSON> inst = compiler.parse("test", expr);
		AsyncExecutionContext<JSON> context = JtlCompiler.createInitialContext(data, getConfig(), new File(".").getCanonicalFile(), builder, les);
		ListenableFuture<JSON> result = inst.call(context, Futures.immediateCheckedFuture(data));
		JSON j = result.get();
		return j;
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
		if(j instanceof JSONArray) {
			JSON expected = builder.parse("[\"fire\",\"ice\",\"candy\"]"); 
			assertEquals(j,expected);
		}
	}

	@Test
	public void testGroup() throws Exception {
		JSON data = getBaseData();
		JSON j = runExpression("people/group(element)/fire/count()", data);
		assertTrue(j.isNumber());
		if (j.isNumber()) {
			assertTrue(((JSONValue) j).longValue() == 2);
//			System.err.println(((JSONValue) j).longValue());
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
