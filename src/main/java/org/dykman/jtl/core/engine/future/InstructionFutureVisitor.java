package org.dykman.jtl.core.engine.future;

import static com.google.common.util.concurrent.Futures.immediateFailedCheckedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.transform;

import java.util.ArrayList;
import java.util.Iterator;
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
import main.antlr.jtlParser.IdentContext;
import main.antlr.jtlParser.IndexlContext;
import main.antlr.jtlParser.IndexlistContext;
import main.antlr.jtlParser.JpathContext;
import main.antlr.jtlParser.JsonContext;
import main.antlr.jtlParser.JstringContext;
import main.antlr.jtlParser.JtlContext;
import main.antlr.jtlParser.Mul_exprContext;
import main.antlr.jtlParser.NumberContext;
import main.antlr.jtlParser.ObjectContext;
import main.antlr.jtlParser.Or_exprContext;
import main.antlr.jtlParser.PairContext;
import main.antlr.jtlParser.PathContext;
import main.antlr.jtlParser.PathelementContext;
import main.antlr.jtlParser.PathindexContext;
import main.antlr.jtlParser.PathstepContext;
import main.antlr.jtlParser.Re_exprContext;
import main.antlr.jtlParser.RecursContext;
import main.antlr.jtlParser.Rel_exprContext;
import main.antlr.jtlParser.Rel_pathContext;
import main.antlr.jtlParser.StrcContext;
import main.antlr.jtlParser.StringContext;
import main.antlr.jtlParser.Tern_exprContext;
import main.antlr.jtlParser.Unary_exprContext;
import main.antlr.jtlParser.Union_exprContext;
import main.antlr.jtlParser.ValueContext;
import main.antlr.jtlParser.VariableContext;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSON.JSONType;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.Pair;
import org.dykman.jtl.core.engine.ExecutionException;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class InstructionFutureVisitor extends
		jtlBaseVisitor<InstructionFutureValue<JSON>> {

	final JSONBuilder builder;
	final InstructionFutureFactory factory;

	public InstructionFutureVisitor(JSONBuilder builder) {
		this.builder = builder;
		this.factory = new InstructionFutureFactory(builder);
	}

	@Override
	public InstructionFutureValue<JSON> visitJtl(JtlContext ctx) {
		ValueContext jc = ctx.value();
		final InstructionFuture<JSON> inst = visitValue(jc).inst;
		return new InstructionFutureValue<JSON>(
				new AbstractInstructionFuture<JSON>() {

					@Override
					public ListenableFuture<JSON> call(
							AsyncExecutionContext<JSON> context,
							ListenableFuture<JSON> data) throws ExecutionException {
						return transform(inst.call(context, data),
								new AsyncFunction<JSON, JSON>() {
									@Override
									public ListenableFuture<JSON> apply(
											JSON input) throws Exception {
//										sealResult(input);
										input.lock();
										return immediateFuture(input);
									}
								});
					}
				});
	}


	protected void sealResult(JSON j) {
		JSONType type = j.getType();
		switch (type) {
		case ARRAY: {
			JSONArray a = (JSONArray) j;
			int index = 0;
			for (JSON k : a) {
				k.setIndex(index++);
				k.setParent(j);
				sealResult(k);
				k.lock();
			}
		}
		case OBJECT: {
			JSONObject a = (JSONObject) j;
			for (Pair<String, JSON> ee : a) {
				ee.s.setName(ee.f);
				ee.s.setParent(j);
				sealResult(ee.s);
				ee.s.lock();
			}
		}
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitJson(JsonContext ctx) {
		return visitValue(ctx.value());
	}

	@Override
	public InstructionFutureValue<JSON> visitObject(ObjectContext ctx) {
		List<Pair<String, InstructionFuture<JSON>>> ins = new ArrayList<>(
				ctx.getChildCount());
		InstructionFutureValue<JSON> pp;
		for (PairContext p : ctx.pair()) {
			pp = visitPair(p);
			ins.add(new Pair<String, InstructionFuture<JSON>>(pp.ninst.f,
					pp.ninst.s));
		}

		try {
			return new InstructionFutureValue<JSON>(factory.object(ins));
		} catch (ExecutionException e) {
			return new InstructionFutureValue<JSON>(
					factory.value("ExecutionException during visitObject: "
							+ e.getLocalizedMessage()));
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
		String ks = k.string;

		InstructionFutureValue<JSON> v = visitValue(ctx.value());
		return new InstructionFutureValue<JSON>(ks, v.inst);
	}

	@Override
	public InstructionFutureValue<JSON> visitArray(ArrayContext ctx) {
		List<InstructionFuture<JSON>> ins = new ArrayList<>(ctx.getChildCount());
		for (ValueContext vc : ctx.value()) {
			ins.add(visitValue(vc).inst);
		}
		return new InstructionFutureValue<JSON>(factory.array(ins));
	}

	@Override
	public InstructionFutureValue<JSON> visitValue(ValueContext ctx) {
		ObjectContext oc = ctx.object();
		if (oc != null) {
			return visitObject(oc);
		}
		ArrayContext ac = ctx.array();
		if (ac != null) {
			return visitArray(ac);
		}
		JpathContext jc = ctx.jpath();
		if (jc != null) {
			return visitJpath(jc);
		}
		throw new RuntimeException("unknown value type");
	}

	@Override
	public InstructionFutureValue<JSON> visitString(StringContext ctx) {
		TerminalNode tn = ctx.STRING();
		String s = tn.getText();
		return new InstructionFutureValue<JSON>(factory.string(s.substring(1,
				s.length() - 1)));
	}

	@Override
	public InstructionFutureValue<JSON> visitFunc(FuncContext ctx) {
		List<InstructionFuture<JSON>> ins = new ArrayList<>(ctx.getChildCount());
		for (ValueContext jc : ctx.value()) {
			ins.add(visitValue(jc).inst);
		}
		return new InstructionFutureValue<JSON>(factory.function(ctx
				.getChild(0).getText(), ins));
	}

	@Override
	public InstructionFutureValue<JSON> visitVariable(VariableContext ctx) {
		String name = ctx.getText();
		char c = name.charAt(0);
		if(c == '!' || c == '$') name = name.substring(1);
		final String nm = name;
		return new InstructionFutureValue<JSON>(
				new AbstractInstructionFuture<JSON>() {

					@Override
					public ListenableFuture<JSON> call(
							AsyncExecutionContext<JSON> context,
							ListenableFuture<JSON> t) {
						try {
							System.out.println("func name: " + nm);
							return context.lookup(nm,t);
						} catch (Exception e) {
							return immediateFailedCheckedFuture(e);
						}
					}
				});
	}

	@Override
	public InstructionFutureValue<JSON> visitId(IdContext ctx) {
		String c = visitIdent(ctx.ident()).string;
		IdContext ic = ctx.id();
		if(ic!=null) {
			String a =visitId(ic).string;
			StringBuilder builder= new StringBuilder(a).append('.').append(c);
			return new InstructionFutureValue<>(builder.toString());
		}
		return new InstructionFutureValue<>(c);
	}


	@Override
	public InstructionFutureValue<JSON> visitIdent(IdentContext ctx) {
		return new InstructionFutureValue<JSON>(ctx.getText());
	}

	@Override
	public InstructionFutureValue<JSON> visitNumber(NumberContext ctx) {
		return new InstructionFutureValue<JSON>(factory.number(ctx.INTEGER(),
				ctx.FLOAT()));
	}

	@Override
	public InstructionFutureValue<JSON> visitJpath(JpathContext ctx) {
		return visitRe_expr(ctx.re_expr());
	}

	@Override
	public InstructionFutureValue<JSON> visitRe_expr(Re_exprContext ctx) {
		
		Tern_exprContext tc = ctx.tern_expr();
		if(tc!=null) return visitTern_expr(tc);
		InstructionFutureValue<JSON> a = visitRe_expr(ctx.re_expr());
		String s = visitString(ctx.string()).string;
		return new InstructionFutureValue<>(factory.reMatch(s,a.inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitTern_expr(Tern_exprContext ctx) {
		Or_exprContext oc = ctx.or_expr();
		if (oc != null)
			return visitOr_expr(oc);
		Tern_exprContext tc = ctx.tern_expr();
		Iterator<ValueContext> vit = ctx.value().iterator();
		ValueContext va = vit.next();
		ValueContext vb = vit.next();
		return new InstructionFutureValue<>(factory.ternary(
				visitTern_expr(tc).inst, 
				visitValue(va).inst,
				visitValue(vb).inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitOr_expr(Or_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitAnd_expr(ctx.and_expr());
		Or_exprContext c = ctx.or_expr();
		if (c != null) {
			return new InstructionFutureValue<JSON>(factory.dyadic(a.inst,
					visitOr_expr(c).inst, new DyadicAsyncFunction<JSON>() {
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
			return new InstructionFutureValue<JSON>(factory.dyadic(a.inst,
					visitAnd_expr(c).inst, new DyadicAsyncFunction<JSON>() {
						@Override
						public JSON invoke(AsyncExecutionContext<JSON> eng,
								JSON a, JSON b) {
							return builder.value(a.isTrue() && b.isTrue());
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
			return new InstructionFutureValue<JSON>(factory.dyadic(a.inst,
					visitEq_expr(c).inst, new DyadicAsyncFunction<JSON>() {
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
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l,
							JSON r) {
						return builder.value(l.compare(r) < 0);
					}
				};
				break;
			case ">":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l,
							JSON r) {
						return builder.value(l.compare(r) > 0);
					}
				};
				break;
			case "<=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l,
							JSON r) {
						return builder.value(l.compare(r) <= 0);
					}
				};
				break;
			case ">=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l,
							JSON r) {
						return builder.value(l.compare(r) >= 0);
					}
				};
				break;
			}
			return new InstructionFutureValue<JSON>(factory.dyadic(a.inst,
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
			InstructionFutureValue<JSON> bv = visitAdd_expr(c);
			String sop = ctx.getChild(1).getText();
			switch (sop) {
			case "+":
				return new InstructionFutureValue<JSON>(factory.addInstruction(
						a.inst, bv.inst));
			case "-":
				return new InstructionFutureValue<>(factory.subInstruction(
						a.inst, bv.inst));
			}
		}
		return a;

	}

	@Override
	public InstructionFutureValue<JSON> visitMul_expr(Mul_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitUnary_expr(ctx.unary_expr());
		Mul_exprContext c = ctx.mul_expr();
		if (c != null) {
			InstructionFutureValue<JSON> bv = visitMul_expr(c);
			String sop = ctx.getChild(1).getText();
			switch (sop) {
			case "*":
				return new InstructionFutureValue<>(factory.mulInstruction(
						a.inst, bv.inst));
			case "div":
				return new InstructionFutureValue<>(factory.divInstruction(
						a.inst, bv.inst));
			case "%":
				return new InstructionFutureValue<>(factory.modInstruction(
						a.inst, bv.inst));
			}
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitUnary_expr(Unary_exprContext ctx) {
		Union_exprContext uc = ctx.union_expr();
		if(uc!=null) return visitUnion_expr(uc);
		InstructionFutureValue<JSON> a = visitUnary_expr(ctx.unary_expr());
		return new InstructionFutureValue<JSON>(factory.negate(a.inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitUnion_expr(Union_exprContext ctx) {
		List<Filter_pathContext> fps = ctx.filter_path();
		if(fps.size() == 1) {
			return visitFilter_path(fps.get(0));
		} else {
			final List<InstructionFuture<JSON>> seq = new ArrayList<>();
			for(Filter_pathContext fp: fps) {
				seq.add(visitFilter_path(fp).inst);
			}
			return new InstructionFutureValue<>(new AbstractInstructionFuture<JSON>() {

				@Override
				public ListenableFuture<JSON> call(
						AsyncExecutionContext<JSON> context,
						ListenableFuture<JSON> data) throws ExecutionException {
					final List<ListenableFuture<JSON>> fut = new ArrayList<>();
					for(InstructionFuture<JSON> ii : seq) {
						fut.add(ii.call(context, data));
					}
					return transform(Futures.allAsList(fut),new AsyncFunction<List<JSON>, JSON>() {

						@Override
						public ListenableFuture<JSON> apply(List<JSON> input)
								throws Exception {
							JSONArray unbound = builder.array(null, true);
							for(JSON j: input) {
								unbound.add(j);
							}
							return immediateFuture(unbound);
						}
						
					});
				}
			});
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitFilter_path(Filter_pathContext ctx) {
		PathContext pc = ctx.path();
		if(pc!=null) return visitPath(pc);
		InstructionFutureValue<JSON> a = visitFilter_path(ctx.filter_path());
		InstructionFutureValue<JSON> b = visitValue(ctx.value());
		return new InstructionFutureValue<>(factory.dereference(a.inst, b.inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitPath(PathContext ctx) {
		Abs_pathContext ac  = ctx.abs_path();
		return visitAbs_path(ac);
	}

	@Override
	public InstructionFutureValue<JSON> visitAbs_path(Abs_pathContext ctx) {
		final InstructionFutureValue<JSON> a = visitRel_path(ctx.rel_path());
		if (1 < ctx.getChildCount()) {
			return new InstructionFutureValue<>(
					new AbstractInstructionFuture<JSON>() {
						@Override
						public ListenableFuture<JSON> call(
								AsyncExecutionContext<JSON> context,
								ListenableFuture<JSON> data)
								throws ExecutionException {
							return transform(data,
									new AsyncFunction<JSON, JSON>() {
										@Override
										public ListenableFuture<JSON> apply(
												JSON input) throws Exception {
											JSON parent = input.getParent();
											while (parent != null) {
												input = parent;
												parent = input.getParent();
											}
											return immediateFuture(input);
										}
									});
						}
					});
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitRel_path(Rel_pathContext ctx) {
		final InstructionFutureValue<JSON> a = visitPathelement(ctx
				.pathelement());
		Rel_pathContext rp = ctx.rel_path();
		if (rp != null) {
			final InstructionFutureValue<JSON> c = visitRel_path(rp);
			return new InstructionFutureValue<JSON>(
					new AbstractInstructionFuture<JSON>() {

						@Override
						public ListenableFuture<JSON> call(
								final AsyncExecutionContext<JSON> context,
								final ListenableFuture<JSON> data)
								throws ExecutionException {
							return a.inst.call(context,
									c.inst.call(context, data));
						}
					});
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitPathelement(PathelementContext ctx) {
		PathstepContext psc = ctx.pathstep();
		if (psc != null) {
			return visitPathstep(psc);
		}
		PathelementContext pc = ctx.pathelement();
		InstructionFutureValue<JSON> pif = visitPathelement(pc);
		InstructionFutureValue<JSON> vif = visitPathindex(ctx.pathindex());
		
		return new InstructionFutureValue<JSON>(factory.dereference(pif.inst,
				vif.inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitPathstep(PathstepContext ctx) {
		VariableContext vc = ctx.variable();
		if (vc != null)
			return visitVariable(vc);

		FuncContext fc = ctx.func();
		if (fc != null)
			return visitFunc(fc);

		NumberContext nc = ctx.number();
		if (nc != null)
			return visitNumber(nc);

		JstringContext jc = ctx.jstring();
		if (jc != null)
			return visitJstring(jc);

		RecursContext rc = ctx.recurs();
		if (rc != null)
			return visitRecurs(rc);

		IdContext ic = ctx.id();
		if (ic != null) {
			String id = visitId(ic).string;
			return new InstructionFutureValue<>(factory.get(id));
		}

		String t = ctx.getText();
		{
			switch (t) {
			case "true":
				return new InstructionFutureValue<>(factory.value(true));
			case "false":
				return new InstructionFutureValue<>(factory.value(false));
			case "null":
				return new InstructionFutureValue<>(factory.value());
			case "*":
				return new InstructionFutureValue<>(factory.stepChildren());
			case ".":
				return new InstructionFutureValue<>(factory.stepSelf());
			case "..":
				return new InstructionFutureValue<>(factory.stepParent());

			}
		}
		throw new RuntimeException("token " + t
				+ " is not implemented in pathstep");
	}

	@Override
	public InstructionFutureValue<JSON> visitRecurs(RecursContext ctx) {
		String t = ctx.getText();
		switch (t) {
		case "**":
			return new InstructionFutureValue<>(factory.recursDown());
		case "...":
			return new InstructionFutureValue<>(factory.recursUp());
		}
		throw new RuntimeException("token " + t
				+ " is not implemented in recurs");
	}

	@Override
	public InstructionFutureValue<JSON> visitJstring(JstringContext ctx) {
		StringContext sc = ctx.string();
		if (sc != null)
			return visitString(sc);
		StrcContext stc = ctx.strc();
		return visitStrc(stc);
	}

	@Override
	public InstructionFutureValue<JSON> visitStrc(StrcContext ctx) {
		JtlContext jtl = ctx.jtl();
		if (jtl != null) {
			return new InstructionFutureValue<JSON>(
					new AbstractInstructionFuture<JSON>() {

						@Override
						public ListenableFuture<JSON> call(
								AsyncExecutionContext<JSON> context,
								ListenableFuture<JSON> data)
								throws ExecutionException {
							return transform(data,
									new AsyncFunction<JSON, JSON>() {

										@Override
										public ListenableFuture<JSON> apply(
												JSON input) throws Exception {
											return immediateFuture(builder
													.value(input.toString()));
										}
									});
						}
					});
		}

		List<StrcContext> strc = ctx.strc();
		if (strc != null && strc.size() > 0) {
			Iterator<StrcContext> sit = strc.iterator();
			StrcContext c1 = sit.next();
			StrcContext c2 = sit.next();
			final InstructionFutureValue<JSON> ci1 = visitStrc(c1);
			final InstructionFutureValue<JSON> ci2 = visitStrc(c2);
			return new InstructionFutureValue<>(
					new AbstractInstructionFuture<JSON>() {

						@Override
						public ListenableFuture<JSON> call(
								AsyncExecutionContext<JSON> context,
								ListenableFuture<JSON> data)
								throws ExecutionException {
							ListenableFuture<JSON> r1 = ci1.inst.call(context,
									data);
							ListenableFuture<JSON> r2 = ci2.inst.call(context,
									data);
							return transform(Futures.allAsList(r1, r2),
									new AsyncFunction<List<JSON>, JSON>() {

										@Override
										public ListenableFuture<JSON> apply(
												List<JSON> input)
												throws Exception {
											Iterator<JSON> jit = input
													.iterator();
											JSONValue s1 = (JSONValue) jit
													.next();
											JSONValue s2 = (JSONValue) jit
													.next();
											String ss1 = s1.getType() == JSONType.NULL ? ""
													: s1.stringValue();
											String ss2 = s2.getType() == JSONType.NULL ? ""
													: s2.stringValue();
											return immediateFuture(builder
													.value(ss1 + ss2));
										}
									});
						}
					});
		}
		final String t = ctx.getText();
		return new InstructionFutureValue<JSON>(
				new AbstractInstructionFuture<JSON>() {

					@Override
					public ListenableFuture<JSON> call(
							AsyncExecutionContext<JSON> context,
							ListenableFuture<JSON> data) throws ExecutionException {
						return immediateFuture(builder.value(t));
					}
				});
	}

	@Override
	public InstructionFutureValue<JSON> visitPathindex(PathindexContext ctx) {
		return visitIndexlist(ctx.indexlist());
	}

	@Override
	public InstructionFutureValue<JSON> visitIndexlist(IndexlistContext ctx) {
		InstructionFutureValue<JSON> a = visitIndexl(ctx.indexl());
		IndexlistContext b = ctx.indexlist();
		if(b!=null) {
			InstructionFutureValue<JSON> bi = visitIndexlist(b);
			return new InstructionFutureValue<JSON>(factory.dereference(a.inst, bi.inst));
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitIndexl(IndexlContext ctx) {
		List<ValueContext> cl = ctx.value();
		final InstructionFuture<JSON> a = visitValue(cl.get(0)).inst;
		if(cl.size() > 0) {
			final InstructionFuture<JSON> b = visitValue(cl.get(1)).inst;
			return new InstructionFutureValue<>(new AbstractInstructionFuture<JSON>() {
				@Override
				public ListenableFuture<JSON> call(
						AsyncExecutionContext<JSON> context,
						ListenableFuture<JSON> data) throws ExecutionException {
					return transform(Futures.allAsList(a.call(context, data),b.call(context, data)),
							new AsyncFunction<List<JSON>, JSON>() {

								@Override
								public ListenableFuture<JSON> apply(
										List<JSON> input) throws Exception {
									JSONArray arr = builder.array(null);
									Iterator<JSON> it = input.iterator();
									arr.add(it.next());
									arr.add(it.next());
									return immediateFuture(arr);
								}
							});
				}
			});
		}
		return new InstructionFutureValue<>(a);
	}

}
