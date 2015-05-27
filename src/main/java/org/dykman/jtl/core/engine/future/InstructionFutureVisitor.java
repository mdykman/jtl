package org.dykman.jtl.core.engine.future;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.antlr.jtlBaseVisitor;
import main.antlr.jtlParser.Abs_pathContext;
import main.antlr.jtlParser.Add_exprContext;
import main.antlr.jtlParser.And_exprContext;
import main.antlr.jtlParser.ArrayContext;
import main.antlr.jtlParser.Eq_exprContext;
import main.antlr.jtlParser.Filter_pathContext;
import main.antlr.jtlParser.FuncContext;
import main.antlr.jtlParser.IdContext;
import main.antlr.jtlParser.JpathContext;
import main.antlr.jtlParser.JsonContext;
import main.antlr.jtlParser.JtlContext;
import main.antlr.jtlParser.Mul_exprContext;
import main.antlr.jtlParser.NumberContext;
import main.antlr.jtlParser.ObjectContext;
import main.antlr.jtlParser.Or_exprContext;
import main.antlr.jtlParser.PairContext;
import main.antlr.jtlParser.PathContext;
import main.antlr.jtlParser.PathelementContext;
import main.antlr.jtlParser.PathstepContext;
import main.antlr.jtlParser.RecursContext;
import main.antlr.jtlParser.Rel_exprContext;
import main.antlr.jtlParser.Rel_pathContext;
import main.antlr.jtlParser.S_exprContext;
import main.antlr.jtlParser.StringContext;
import main.antlr.jtlParser.Unary_exprContext;
import main.antlr.jtlParser.Union_exprContext;
import main.antlr.jtlParser.ValueContext;
import main.antlr.jtlParser.VariableContext;

import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.parser.JSONBuilder;

import static com.google.common.util.concurrent.Futures.*;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

public class InstructionFutureVisitor extends
		jtlBaseVisitor<InstructionFutureValue<JSON>> {

	JSONBuilder builder;
	InstructionFutureFactory factory;
	public InstructionFutureVisitor(JSONBuilder builder) {
		this.builder = builder;
		factory = new InstructionFutureFactory(builder);
	}
	@Override
	public InstructionFutureValue<JSON> visitJtl(JtlContext ctx) {
		JsonContext jc = ctx.json();
		return visitJson(jc);
	}

	@Override
	public InstructionFutureValue<JSON> visitJson(JsonContext ctx) {
		return visitValue(ctx.value());
	}

	@Override
	public InstructionFutureValue<JSON> visitObject(ObjectContext ctx) {
		List<Duo<String, InstructionFuture<JSON>>> ins = new ArrayList<>(ctx.getChildCount());
		InstructionFutureValue<JSON> pp;
		for (PairContext p : ctx.pair()) {
			pp = visitPair(p);
			ins.add(new Duo<String, InstructionFuture<JSON>>(pp.ninst.first,
					pp.ninst.second));
		}

		try {
			return new InstructionFutureValue<JSON>(
					factory.object(ins));
		} catch (JSONException e) {
			return new InstructionFutureValue<JSON>(
					factory.value("JSONException during visitObject: " + e.getLocalizedMessage()));
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitPair(PairContext ctx) {
		InstructionFutureValue<JSON> k = null;
		IdContext id = ctx.id();
		if (id != null) {
			k = visitId(id);
		} else {
			k = visitString(ctx.string());
		}
		ListenableFuture<JSON> kj;
		try {
			// TODO:: WTF??!!
			// what the hell am i doing here???
			kj = k.inst.call(null,null);
		} catch (JSONException e) {
			return new InstructionFutureValue<JSON>(
					factory.value("JSONException during visitPair: " + e.getLocalizedMessage()));
		}
		InstructionFutureValue<JSON> v = visitValue(ctx.value());
		return new InstructionFutureValue<JSON>(kj.toString(), v.inst);
	}

	@Override
	public InstructionFutureValue<JSON> visitArray(ArrayContext ctx) {
		List<InstructionFuture<JSON>> ins = new ArrayList<>(ctx.getChildCount());
		for (ValueContext vc : ctx.value()) {
			ins.add(visitValue(vc).inst);
		}
		return new InstructionFutureValue<JSON>(
				factory.array(ins));
	}

	@Override
	public InstructionFutureValue<JSON> visitValue(ValueContext ctx) {
		ObjectContext oc = ctx.object();
		if(oc!=null) {
			return visitObject(oc);
		}
		ArrayContext ac = ctx.array();
		if(ac!=null) {
			return visitArray(ac);
		}
		JpathContext jc = ctx.jpath();
		if(jc!=null) {
			return visitJpath(jc);
		}
		throw new RuntimeException("unknown value type");
	}

	@Override
	public InstructionFutureValue<JSON> visitString(StringContext ctx) {
		String t = ctx.STRING().getText();
		final String s = t.substring(1, t.length()-1);
		InstructionFuture<JSON> in = new InstructionFuture<JSON>() {
			@Override
			public ListenableFuture<JSON> call(
					AsyncExecutionContext<JSON> context,
					ListenableFuture<JSON> parent) throws JSONException {
				return immediateFuture(builder.value(s));
			}
		};
		return super.visitString(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitS_expr(S_exprContext ctx) {
		JsonContext jc = ctx.json();
		return visitJson(jc);
	}

	@Override
	public InstructionFutureValue<JSON> visitFunc(FuncContext ctx) {
		List<InstructionFuture<JSON>> ins = new ArrayList<>(ctx.getChildCount());
		for (JsonContext jc : ctx.json()) {
			ins.add(visitJson(jc).inst);
		}
		return new InstructionFutureValue<JSON>(
				factory.function(ctx.getChild(0).getText(),
						ins));
	}

	@Override
	public InstructionFutureValue<JSON> visitVariable(VariableContext ctx) {
		final String name = ctx.i.getText();
		return new InstructionFutureValue<JSON>(
				new AbstractInstructionFuture<JSON>() {

					@Override
					public ListenableFuture<JSON> call(
							AsyncExecutionContext<JSON> context,
							ListenableFuture<JSON> parent) {
						try {
						return context.lookup(name);
						} catch(Exception e) {
							return immediateFailedCheckedFuture(e);
						}
					}
				});
	}

	@Override
	public InstructionFutureValue<JSON> visitId(IdContext ctx) {
		return new InstructionFutureValue<JSON>(
				factory.string(ctx.getText()));
	}

	@Override
	public InstructionFutureValue<JSON> visitNumber(NumberContext ctx) {
		return new InstructionFutureValue<JSON>(
				factory.number(ctx.INTEGER(),ctx.FLOAT()));
	}

	@Override
	public InstructionFutureValue<JSON> visitJpath(JpathContext ctx) {
		return super.visitJpath(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitOr_expr(Or_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitAnd_expr(ctx.and_expr());
		Or_exprContext c = ctx.or_expr();
		if (c != null) {
			return new InstructionFutureValue<JSON>(
					factory.dyadic(a.inst,
							visitOr_expr(c).inst,
							new DyadicAsyncFunction<JSON>() {
								@Override
								public JSON invoke(AsyncExecutionContext<JSON> eng,
										JSON a, JSON b) {
									return a.isTrue() ? a : b;
								}
							}));
		} else {
			return a;
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitAnd_expr(And_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitEq_expr(ctx.eq_expr());
		And_exprContext c = ctx.and_expr();
		if (c != null) {
			return new InstructionFutureValue<JSON>(
					factory.dyadic(a.inst,
							visitAnd_expr(c).inst,
							new DyadicAsyncFunction<JSON>() {
								@Override
								public JSON invoke(AsyncExecutionContext<JSON> eng,
										JSON a, JSON b) {
									return builder.value(a.isTrue()
											&& b.isTrue());
								}
							}));
		} else {
			return a;
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitEq_expr(Eq_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitRel_expr(ctx.rel_expr());
		Eq_exprContext c = ctx.eq_expr();

		if (c != null) {
			final boolean inv = ctx.getChild(1).getText().equals("!=");
			return new InstructionFutureValue<JSON>(
					factory.dyadic(a.inst,
							visitEq_expr(c).inst,
							new DyadicAsyncFunction<JSON>() {
								@Override
								public JSON invoke(AsyncExecutionContext<JSON> eng,
										JSON a, JSON b) {
									boolean e = a.equals(b);
									return builder.value(inv ^ e);
								}
							}));

		} else {
			return a;
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitRel_expr(Rel_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitAdd_expr(ctx.add_expr());
		Rel_exprContext c = ctx.rel_expr();
		if (c != null) {
			DyadicAsyncFunction<JSON> df = null;
			switch (ctx.getChild(1).getText()) {
			case "<":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compare(r) < 0);
					}
				};
				break;
			case ">":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compare(r) > 0);
					}
				};
				break;
			case "<=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compare(r) <= 0);
					}
				};
				break;
			case ">=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compare(r) >= 0);
					}
				};
				break;
			}
			return new InstructionFutureValue<JSON>(
					factory.dyadic(a.inst,
							visitRel_expr(c).inst, df));
		} else {
			return a;
		}
	}
	
	protected InstructionFuture<JSON> addInstruction(InstructionFutureValue<JSON> a,Add_exprContext c) {
		return factory.dyadic(a.inst,
				visitAdd_expr(c).inst,
				new DefaultPolymorphicOperator(builder) {
					@Override public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {return l+r; }
					@Override public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {return l+r; }
					@Override public String op(AsyncExecutionContext<JSON> eng, String l, String r) {return l+r; }
					@Override public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONArray r) {
//						Collection<JSON> cc = builder.collection();
						JSONArray arr = builder.array(null);
						// this needs to be a deep clone for the internal referencing to hold.
						int i = 0;
						for(JSON j : l.collection()) {
							arr.add(j);
						}
						for(JSON j : r.collection()) {
							arr.add(j);
						}
						return arr;
					}
					@Override public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
						JSONArray arr = builder.array(null);
						// this needs to be a deep clone for the internal referencing to hold.
						int i = 0;
						for(JSON j : l.collection()) {
							arr.add(j);
						}
						arr.add(r);
						return arr;
					}
					@Override public JSONObject op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONObject r) {
						JSONObject obj = builder.object(null);
						for(Map.Entry<String, JSON> ee: r.map().entrySet()) {
							String k = ee.getKey();
							JSON j = ee.getValue();
							obj.put(k, j);
						}
						for(Map.Entry<String, JSON> ee: l.map().entrySet()) {
							String k = ee.getKey();
							JSON j = ee.getValue();
							obj.put(k, j);
						}
						return obj;
					}
				});
	}
	
	protected InstructionFuture<JSON> subInstruction(InstructionFutureValue<JSON> a,Add_exprContext c) {
		return factory.dyadic(a.inst,
				visitAdd_expr(c).inst,
				new DefaultPolymorphicOperator(builder) {
					@Override public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {return l-r; }
					@Override public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {return l-r; }
					@Override public String op(AsyncExecutionContext<JSON> eng, String l, String r) {
						int n = l.indexOf(r);
						if(n!=-1) {
							StringBuilder b = new StringBuilder(l.substring(0, n));
							b.append(l.substring(n+r.length()));
							return b.toString();
						}
						return r;
					}
					@Override public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONArray r) {
						JSONArray arr = builder.array(null);
						// this needs to be a deep clone for the internal referencing to hold.
						int i = 0;
						for(JSON j : l.collection()) {
							if(!r.contains(j)) {
								arr.add(j);
							}
						}
						return arr;
					}
					@Override public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
						JSONArray arr = builder.array(null);
						// this needs to be a deep clone for the internal referencing to hold.
						int i = 0;
						for(JSON j : l.collection()) {
							if(!j.equals(r)) {
								arr.add(j);
							}
						}
						return arr;
					}
					@Override public JSONObject op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONObject r) {
						Map<String,JSON> m = builder.map();
						JSONObject obj = builder.object(null,m);
						for(Map.Entry<String, JSON> ee: r.map().entrySet()) {
							String k = ee.getKey();
							JSON j = ee.getValue();
							if(!r.containsKey(k)) obj.put(k, j);
						}
						return obj;
					}
				});
	}

	@Override
	public InstructionFutureValue<JSON> visitAdd_expr(Add_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitMul_expr(ctx.mul_expr());
		Add_exprContext c = ctx.add_expr();
		if (c != null) {
			String sop = ctx.getChild(1).getText();
			switch(sop) {
			case "+":			
			return new InstructionFutureValue<>(
					addInstruction(a, c));
			case "-":			
			return new InstructionFutureValue<>(
					subInstruction(a, c));
			}
		} 
		return a;
		
		} 
	protected InstructionFuture<JSON> mulInstruction(
			InstructionFutureValue<JSON> a,
			Mul_exprContext c) {
		return factory.dyadic(a.inst,
				visitMul_expr(c).inst,
				new DefaultPolymorphicOperator(builder) {
					@Override public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {return l*r; }
					@Override public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {return l*r; }
					/*
					@Override public String op(AsyncExecutionContext<JSON> eng, String l, String r) {
						int n = l.indexOf(r);
						if(n!=-1) {
							StringBuilder b = new StringBuilder(l.substring(0, n));
							b.append(l.substring(n+r.length()));
							return b.toString();
						}
						return r;
					}
					@Override public JSON op(AsyncExecutionContext<JSON> eng, JSONArray l, JSONArray r) {
						if(r.size()!=l.size()) return new JSONValue(null);
						Collection<JSON> cc = builder.collection();
						JSONArray arr = builder.array(null, cc);
						// this needs to be a deep clone for the internal referencing to hold.
						Iterator<JSON> rit = r.iterator();
						for(JSON j : l.collection()) {
							JSON rj = rit.next();
							if(j.isNumber() && r.isNumber()) {
							
							} else {
								j = new JSONValue(arr);
							}
								j.setParent(arr);
								j.setIndex(i++);
								cc.add(j);
						}
						return arr;
					}
					@Override public JSONArray op(AsyncExecutionContext<JSON> eng, JSONArray l, JSON r) {
						Collection<JSON> cc = builder.collection();
						JSONArray arr = builder.array(null, cc);
						// this needs to be a deep clone for the internal referencing to hold.
						int i = 0;
						for(JSON j : l.collection()) {
							if(!j.equals(r)) {
								j = j.cloneJSON();
								j.setParent(arr);
								j.setIndex(i++);
								j.lock();
								cc.add(j);
							}
						}
						r = r.cloneJSON();
						r.setParent(arr);
						r.setIndex(i++);
						r.lock();
						cc.add(r);
						arr.lock();
						return arr;
					}
					@Override public JSONObject op(AsyncExecutionContext<JSON> eng, JSONObject l, JSONObject r) {
						Map<String,JSON> m = builder.map();
						JSONObject obj = builder.object(null,m);
						for(Map.Entry<String, JSON> ee: r.map().entrySet()) {
							String k = ee.getKey();
							JSON j = ee.getValue().cloneJSON();
							j.setParent(obj);
							j.setName(k);
							j.lock();
							m.put(k,j);
						}
						for(Map.Entry<String, JSON> ee: l.map().entrySet()) {
							String k = ee.getKey();
							JSON j = ee.getValue().cloneJSON();
							j.setParent(obj);
							j.setName(k);
							j.lock();
							m.put(k,j);
						}
						obj.lock();
						return obj;
					}
				});
				*/
		});
	
	}
	protected InstructionFuture<JSON> divInstruction(
			InstructionFutureValue<JSON> a,
			Mul_exprContext c) {
		return factory.dyadic(a.inst,
				visitMul_expr(c).inst,
				new DefaultPolymorphicOperator(builder) {
					@Override public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {return l/r; }
					@Override public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {return l/r; }
		});
	
	}
	
	protected InstructionFuture<JSON> modInstruction(
			InstructionFutureValue<JSON> a,
			Mul_exprContext c) {
		return factory.dyadic(a.inst,
				visitMul_expr(c).inst,
				new DefaultPolymorphicOperator(builder) {
					@Override public Double op(AsyncExecutionContext<JSON> eng, Double l, Double r) {return l%r; }
					@Override public Long op(AsyncExecutionContext<JSON> eng, Long l, Long r) {return l%r; }
		});
	}
	@Override
	public InstructionFutureValue<JSON> visitMul_expr(Mul_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitUnary_expr(ctx.unary_expr());
		Mul_exprContext c = ctx.mul_expr();
		if(c!=null) {
			String sop = ctx.getChild(1).getText();
			switch(sop) {
			case "*":
				return new InstructionFutureValue<>(
						mulInstruction(a, c));
			case "div":
				return new InstructionFutureValue<>(
						divInstruction(a, c));
			case"%":
				return new InstructionFutureValue<>(
						modInstruction(a, c));
			}
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitUnary_expr(Unary_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitUnion_expr(ctx.union_expr());
		Unary_exprContext c = ctx.unary_expr();
		if(c!=null) {
			
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitUnion_expr(Union_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitFilter_path(ctx.filter_path());
		Union_exprContext c = ctx.union_expr();
		if(c!=null) {
			
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitFilter_path(Filter_pathContext ctx) {
		InstructionFutureValue<JSON> a = visitPath(ctx.path());
		Filter_pathContext c = ctx.filter_path();
		if(c!=null) {
			
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitPath(PathContext ctx) {
		return visitAbs_path(ctx.abs_path());
	}

	@Override
	public InstructionFutureValue<JSON> visitAbs_path(Abs_pathContext ctx) {
		final InstructionFutureValue<JSON> a = visitRel_path(ctx.rel_path());
		if(1 < ctx.getChildCount()) {
			return new InstructionFutureValue<>(
					new AbstractInstructionFuture<JSON>() {
						@Override
						public ListenableFuture<JSON> call(
								AsyncExecutionContext<JSON> context,
								ListenableFuture<JSON> data)
								throws JSONException {
							return transform(data, new AsyncFunction<JSON, JSON>() {
								@Override
								public ListenableFuture<JSON> apply(JSON input)
										throws Exception {
									JSON parent = input.getParent();
									while(parent!=null) {
										input = parent;
										parent=input.getParent();
									}
									return immediateFuture(input);
								}
							});
						}
					});
		}
		return a;
	}
// TODO:: the hard work
	@Override
	public InstructionFutureValue<JSON> visitRel_path(Rel_pathContext ctx) {
		final InstructionFutureValue<JSON> a = visitPathelement(ctx.pathelement());
		final InstructionFutureValue<JSON> c = visitRel_path(ctx.rel_path());
		if(c!=null) {
			return new InstructionFutureValue<JSON>(new AbstractInstructionFuture<JSON>() {

				@Override
				public ListenableFuture<JSON> call(
						final AsyncExecutionContext<JSON> context,
						final ListenableFuture<JSON> data) throws JSONException {
					return a.inst.call(context, c.inst.call(context, data));
				}				
			});
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitPathelement(PathelementContext ctx) {
		return super.visitPathelement(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitPathstep(PathstepContext ctx) {
		return super.visitPathstep(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitRecurs(RecursContext ctx) {
		return super.visitRecurs(ctx);
	}

}
