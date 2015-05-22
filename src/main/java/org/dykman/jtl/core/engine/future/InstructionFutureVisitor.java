package org.dykman.jtl.core.engine.future;

import java.util.ArrayList;
import java.util.List;

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
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.parser.JSONBuilder;

import static com.google.common.util.concurrent.Futures.*;
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
				return immediateFuture(new JSONValue(null,s));
			}
		};
		return super.visitString(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitS_expr(S_exprContext ctx) {
		JsonContext jc = ctx.json();
		return visitJson(jc);
//		return super.visitS_expr(ctx);
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
				factory.number(ctx.num));
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
									return eng.engine().bool(a) ? a : b;
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
									return new JSONValue(null, eng.engine().bool(a)
											&& eng.engine().bool(b));
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
			boolean inv = ctx.getChild(1).getText().equals("!=");
			return new InstructionFutureValue<JSON>(
					factory.dyadic(a.inst,
							visitEq_expr(c).inst,
							new DyadicAsyncFunction<JSON>() {
								@Override
								public JSON invoke(AsyncExecutionContext<JSON> eng,
										JSON a, JSON b) {
									boolean e = eng.engine().equals(a, b);
									return new JSONValue(null,
											((inv || e) && !(inv && e)));
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
						return new JSONValue(null, eng.engine().compare(l, r) < 0);
					}
				};
				break;
			case ">":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return new JSONValue(null, eng.engine().compare(l, r) > 0);
					}
				};
				break;
			case "<=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return new JSONValue(null, eng.engine().compare(l, r) <= 0);
					}
				};
				break;
			case ">=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return new JSONValue(null, eng.engine().compare(l, r) >= 0);
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

	@Override
	public InstructionFutureValue<JSON> visitAdd_expr(Add_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitMul_expr(ctx.mul_expr());
		Add_exprContext c = ctx.add_expr();
		if (c != null) {
			return new InstructionFutureValue<JSON>(
					factory.dyadic(a.inst,
							visitAdd_expr(c).inst,
							new DyadicAsyncFunction<JSON>() {
								@Override
								public JSON invoke(AsyncExecutionContext<JSON> eng,
										JSON l, JSON r) {
									Number ln = eng.engine().number(l);
									Number rn = eng.engine().number(r);
									if (ln != null && rn != null) {
										// TODO get real value when i have a
										// generalized arithmetic pattern.
										return new JSONValue(l, 0L);
									}
									return new JSONValue(null, eng.engine()
											.compare(l, r) >= 0);
								}
							}));

		} else {
			return a;
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitMul_expr(Mul_exprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitMul_expr(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitUnary_expr(Unary_exprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitUnary_expr(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitUnion_expr(Union_exprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitUnion_expr(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitFilter_path(Filter_pathContext ctx) {
		// TODO Auto-generated method stub
		return super.visitFilter_path(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitPath(PathContext ctx) {
		// TODO Auto-generated method stub
		return super.visitPath(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitAbs_path(Abs_pathContext ctx) {
		// TODO Auto-generated method stub
		return super.visitAbs_path(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitRel_path(Rel_pathContext ctx) {
		// TODO Auto-generated method stub
		return super.visitRel_path(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitPathelement(PathelementContext ctx) {
		// TODO Auto-generated method stub
		return super.visitPathelement(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitPathstep(PathstepContext ctx) {
		// TODO Auto-generated method stub
		return super.visitPathstep(ctx);
	}

	@Override
	public InstructionFutureValue<JSON> visitRecurs(RecursContext ctx) {
		// TODO Auto-generated method stub
		return super.visitRecurs(ctx);
	}

}
