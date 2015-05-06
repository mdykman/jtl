package org.dykman.jtl.core.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.parser.InstructionFutureValue;
import org.dykman.jtl.core.parser.InstructionValue;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.util.concurrent.Futures.*;

public class InstructionFutureFactory {

	public static InstructionFuture<JSON> variable(final String name) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(AsyncEngine<JSON> eng,
					ListenableFuture<JSON> t) {
				return eng.lookup(name);
			}
		};
	}

	public static InstructionFuture<JSON> function(final String name,
			final List<InstructionFuture<JSON>> iargs) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(AsyncEngine<JSON> eng,
					ListenableFuture<JSON> t) {
				List<ListenableFuture<JSON>> a = new ArrayList<>();
				for (InstructionFuture<JSON> i : iargs) {
					a.add(i.call(eng, t));
				}
				return eng.call(name, a, t);
			}
		};
	}

	public static InstructionFuture<JSON> value(final Object val) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(AsyncEngine<JSON> eng,
					ListenableFuture<JSON> t) {
				return transform(t, new AsyncFunction<JSON, JSON>() {

					@Override
					public ListenableFuture<JSON> apply(JSON input)
							throws Exception {
						return immediateFuture(new JSONValue(input, val));
					}
				});
			}
		};
	}

	public static InstructionFuture<JSON> nil() {
		return value(null);
	}

	public static InstructionFuture<JSON> bool(final Boolean b) {
		return value(b);
	}

	public static InstructionFuture<JSON> string(final String str) {
		return value(str);
	}

	public static InstructionFuture<JSON> number(final Number num) {
		return value(num);
	}

	public static InstructionFuture<JSON> array(
			final List<InstructionFuture<JSON>> ch) {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(AsyncEngine<JSON> eng,
					ListenableFuture<JSON> t) {
				List<ListenableFuture<JSON>> args = new ArrayList<>();
				for (InstructionFuture<JSON> i : ch) {
					args.add(i.call(eng, t));
				}
				return transform(allAsList(args),
						new AsyncFunction<List<JSON>, JSON>() {
							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws Exception {
								JSONArray array = new JSONArray();
								for (JSON j : input) {
									j.setParent(array);
									array.add(j);
								}
								return immediateFuture(array);
							}

						});
			}
		};
	}

	public static InstructionFuture<JSON> dyadic(
			InstructionFuture<JSON> left,
			InstructionFuture<JSON> right,
			DyadicAsyncFunction<JSON> f) {
		return new AbstractInstructionFuture<JSON>() {

			@Override
			public ListenableFuture<JSON> call(AsyncEngine<JSON> eng,
					ListenableFuture<JSON> parent) {
				return transform(allAsList(left.call(eng, parent),right.call(eng, parent)), 
						new KeyedAsyncFunction<List<JSON>, JSON, DyadicAsyncFunction<JSON>>(f) {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws Exception {
								Iterator<JSON> it = input.iterator();
								JSON l = it.next();
								JSON r = it.next();
								return immediateFuture(k.invoke(eng,l, r));
							}
						});
			}
		};
		
	}

	public static InstructionFuture<JSON> object(
			final List<Duo<String, InstructionFuture<JSON>>> ll) {
		return new AbstractInstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(AsyncEngine<JSON> eng,
					ListenableFuture<JSON> t) {
				JSONObject object = (JSONObject) new JSONObject();
				List<ListenableFuture<JSON>> pp = new ArrayList<>();
				for (Duo<String, InstructionFuture<JSON>> p : ll) {
					ListenableFuture<JSON> vl = p.second.call(eng, t);
					pp.add(transform(
							vl,
							new KeyedPayloadAsyncFunction<JSON, JSON, String, JSONObject>(
									p.first, object) {
								@Override
								public ListenableFuture<JSON> apply(JSON input)
										throws Exception {
									input.setParent(p);
									p.put(k, input);
									return immediateFuture(input);
								}
							}));
				}
				return transform(allAsList(pp),
						new KeyedAsyncFunction<List<JSON>, JSON, JSONObject>(
								object) {

							@Override
							public ListenableFuture<JSON> apply(List<JSON> input)
									throws Exception {
								return immediateFuture(k);
							}

						});
			}
		};
	}
}
