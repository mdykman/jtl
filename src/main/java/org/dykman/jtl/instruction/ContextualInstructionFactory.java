package org.dykman.jtl.instruction;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.operator.AbstractFutureInstruction;
import org.dykman.jtl.operator.FutureInstruction;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class ContextualInstructionFactory {

	public static interface Op<T> {
		public T apply(AsyncExecutionContext<T> context, T data, T... vargs);
	}

	public static interface VoidOp<T> {
		public void apply(AsyncExecutionContext<T> context, T data, T... vargs);
	}

	public static interface Nop<T> {
		public T apply(AsyncExecutionContext<T> context, T... vargs);
	}

	public static interface VoidNop<T> {
		public void apply(AsyncExecutionContext<T> context, T... vargs);
	}

	public static abstract class OpportunisticIterator implements Op<JSON> {

		@Override
		public JSON apply(AsyncExecutionContext<JSON> context, JSON data, JSON... vargs) {
			return _apply(context, data, true, vargs);
		}

		public JSON _apply(AsyncExecutionContext<JSON> context, JSON data, boolean recurse, JSON... vargs) {
			JSONType type = data.getType();
			if (type == JSON.JSONType.ARRAY || type == JSON.JSONType.LIST) {
				JSONArray array = context.builder().array(null);
				for (JSON j : ((JSONArray) data)) {
					array.add(f(context, j, vargs));
				}
				return array;
			} else {
				return f(context, data, vargs);

			}
		}

		public abstract JSON f(AsyncExecutionContext<JSON> context, JSON data, JSON... vargs);
	}

	public static abstract class PassiveObjectPairs implements VoidNop<JSON> {

		@Override
		public void apply(AsyncExecutionContext<JSON> context, JSON... vargs) {
			apply(context, true, vargs);
		}

		public void apply(AsyncExecutionContext<JSON> context, boolean recurse, JSON... vargs) {
			int sz = vargs.length;
			if (sz == 1) {
				JSON arg = vargs[0];
				JSON.JSONType type = arg.getType();
				if (recurse && type.equals(JSON.JSONType.ARRAY)) {
					JSONArray array = (JSONArray) arg;
					for (JSON j : array) {
						apply(context, false, j);
					}
				} else if (type.equals(JSON.JSONType.OBJECT)) {
					JSONBuilder builder = context.builder();
					JSONObject jobj = (JSONObject) arg;
					for (Pair<String, JSON> p : jobj) {
						pair(context, builder.value(p.f), p.s);
					}
				}
			} else if (sz == 2) {
				pair(context, vargs[0], vargs[1]);
			}
			// TODO Auto-generated method stub

		}

		public abstract void pair(AsyncExecutionContext<JSON> context, JSON f, JSON s);

		// FutureInstruction<JSON>
	}

	public static FutureInstruction<JSON> trueNot(final SourceInfo meta,
			final FutureInstruction<JSON> arg) {
		return new AbstractFutureInstruction(meta) {
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> data)
					throws ExecutionException {
				return transform(arg.call(context, data),new AsyncFunction<JSON,JSON>() {
					@Override
					public ListenableFuture<JSON> apply(JSON input) throws Exception {
						Boolean b = !input.isTrue();
						return immediateCheckedFuture(context.builder().value(!b));
					}
				});
			
			}
		};
		
	}
	public static FutureInstruction<JSON> trueOr(final SourceInfo meta,
			final FutureInstruction<JSON> lhs, final FutureInstruction<JSON>rhs) {
		return new AbstractFutureInstruction(meta) {
			
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context, final ListenableFuture<JSON> data)
					throws ExecutionException {
					return transform(lhs.call(context, data),new AsyncFunction<JSON, JSON>() {
						@Override
						public ListenableFuture<JSON> apply(JSON input) throws Exception {
							if(input.isTrue()) {
								return immediateCheckedFuture(input);
							} else {
								return rhs.call(context, data);							}
							
						}
					});
			}
		};
	}
	
	public static FutureInstruction<JSON> contextualDataAwareVarArgInstruction(final SourceInfo meta,
			final Op<JSON> vp) {
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				{
					ll.add(data);
					int cc = 1;
					FutureInstruction<JSON> arg = context.getdef(Integer.toString(cc));
					while (arg != null) {
						ll.add(arg.call(context, data));
						arg = context.getdef(Integer.toString(++cc));
					}
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSON _data = input.get(0);
						JSON[] _args = Arrays.copyOfRange(input.toArray(new JSON[0]), 1, input.size());
						return immediateCheckedFuture(vp.apply(context, _data, _args));
					}
				});
			};
		};
	}

	public static FutureInstruction<JSON> contextualDataAwarePassthroughVarArgInstruction(final SourceInfo meta,
			final VoidOp<JSON> vp) {
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				{
					ll.add(data);
					int cc = 1;
					FutureInstruction<JSON> arg = context.getdef(Integer.toString(cc));
					while (arg != null) {
						ll.add(arg.call(context, data));
						arg = context.getdef(Integer.toString(++cc));
					}
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						JSON _data = input.get(0);
						JSON[] _args = Arrays.copyOfRange(input.toArray(new JSON[0]), 1, input.size());
						vp.apply(context, _data, _args);
						return data;
					}
				});
			};
		};
	}

	public static FutureInstruction<JSON> contextualVarArgInstruction(final SourceInfo meta, final Nop<JSON> vp) {
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				{
					int cc = 1;
					FutureInstruction<JSON> arg = context.getdef(Integer.toString(cc));
					while (arg != null) {
						ll.add(arg.call(context, data));
						arg = context.getdef(Integer.toString(++cc));
					}
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						return immediateCheckedFuture(vp.apply(context, input.toArray(new JSON[0])));
					}
				});
			};
		};
	}

	public static FutureInstruction<JSON> contextualPassthroughVarArgInstruction(final SourceInfo meta,
			final VoidNop<JSON> vp) {
		return new AbstractFutureInstruction(meta) {
			@Override
			public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
					final ListenableFuture<JSON> data) throws ExecutionException {
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				{
					int cc = 1;
					FutureInstruction<JSON> arg = context.getdef(Integer.toString(cc));
					while (arg != null) {
						ll.add(arg.call(context, data));
						arg = context.getdef(Integer.toString(++cc));
					}
				}
				return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
					@Override
					public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
						vp.apply(context, input.toArray(new JSON[0]));
						return data;
					}
				});
			};
		};
	}

}
