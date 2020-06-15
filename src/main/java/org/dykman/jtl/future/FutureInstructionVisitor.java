package org.dykman.jtl.future;

import static org.dykman.jtl.operator.FutureInstructionFactory.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.apache.commons.lang3.StringEscapeUtils;
import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.jtlBaseVisitor;

import org.dykman.jtl.jtlParser.*;
import org.dykman.jtl.instruction.ContextualInstructionFactory;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.operator.*;
import org.dykman.jtl.operator.ObjectInstructionBase.ObjectKey;
import org.dykman.jtl.json.*;

import org.dykman.jtl.json.JSON.JSONType;

public class FutureInstructionVisitor extends jtlBaseVisitor<FutureInstructionValue<JSON>> {

	final JSONBuilder builder;
	volatile boolean imported = false;
	final String source;

	public FutureInstructionVisitor(String source, JSONBuilder builder) {
		this(source, builder, false);
	}

	public FutureInstructionVisitor(JSONBuilder builder) {
		this(null, builder, false);
	}

	public FutureInstructionVisitor(String source, JSONBuilder builder, boolean imported) {
		this.builder = builder;
		this.imported = imported;
		this.source = source;
	}

	@Override
	public FutureInstructionValue<JSON> visitJtl(JtlContext ctx) {
		ValueContext jc = ctx.value();
		return visitValue(jc);
	}

	protected void sealResult(JSON j) {
		JSONType type = j.getType();
		switch (type) {
		case LIST:
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
			break;
		case OBJECT: {
			JSONObject a = (JSONObject) j;
			for (Pair<String, JSON> ee : a) {
				ee.s.setName(ee.f);
				ee.s.setParent(j);
				sealResult(ee.s);
				ee.s.lock();
			}
		}
			break;
		default:
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitJson(JsonContext ctx) {
		return visitValue(ctx.value());
	}

	@Override
	public FutureInstructionValue<JSON> visitObject(ObjectContext ctx) {
		List<Pair<ObjectKey, FutureInstruction<JSON>>> ins = new ArrayList<>(ctx.getChildCount());
		FutureInstructionValue<JSON> pp;
		for (PairContext p : ctx.pair()) {
			pp = visitPair(p);
			ins.add(new Pair<ObjectKey, FutureInstruction<JSON>>(pp.ninst.f, pp.ninst.s));
		}

		try {
			return new FutureInstructionValue<JSON>(object(getSource(ctx), ins, false));
		} catch (ExecutionException e) {
			return new FutureInstructionValue<JSON>(value(
				"ExecutionException during visitObject: " + e.getLocalizedMessage(), builder, getSource(ctx)));
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitPair(PairContext ctx) {
		FutureInstructionValue<JSON> k = null;
		KeyContext id = ctx.key();
		if (id != null) {
			k = visitKey(id);
		}
		ObjectKey ks = k.key;

		FutureInstructionValue<JSON> v = visitValue(ctx.value());
		return new FutureInstructionValue<JSON>(ks, v.inst);
	}

	@Override
	public FutureInstructionValue<JSON> visitKey(KeyContext ctx) {
		IdentContext ic = ctx.ident();
		if (ic != null) {
			String s = visitIdent(ic).string;
			if (ctx.getChildCount() > 1) {
				s = ctx.getChild(0).getText() + s;
				// s = ctx.getChild(0).getText() + "." + s;
				// return new
				// FutureInstructionValue<JSON>(ctx.getChild(0).getText() + s);
			}
			return new FutureInstructionValue<JSON>(new ObjectKey(s, false));
		} 
		ValueContext vc = ctx.value(); 
		if(vc!=null) {
			FutureInstruction<JSON> inst = visitValue(vc).inst;
			return new FutureInstructionValue<JSON>(new ObjectKey(inst));
		}
		return new FutureInstructionValue<JSON>(new ObjectKey(visitString(ctx.string()).string, true));
	}

	@Override
	public FutureInstructionValue<JSON> visitArray(ArrayContext ctx) {
		List<FutureInstruction<JSON>> ins = new ArrayList<>(ctx.getChildCount());
		IndexlistContext ind = ctx.indexlist();
		if (ind != null) {
			return visitIndexlist(ind);
		}
		return new FutureInstructionValue<JSON>(array(ins, getSource(ctx)));
	}

	@Override
	public FutureInstructionValue<JSON> visitValue(ValueContext ctx) {
		JpathContext jc = ctx.jpath();
		return visitJpath(jc);
	}

	@Override
	public FutureInstructionValue<JSON> visitString(StringContext ctx) {
		TerminalNode tn = ctx.STRING();
		String s = null;
		if (tn != null) {
			s = tn.getText();
			s = s.substring(1, s.length() - 1);
		} else {
			tn = ctx.SSTRING();
			s = tn.getText();
			s = s.substring(1, s.length() - 1);
		}
//		System.out.println("visitString before " + s);
		if (s != null)
			s = StringEscapeUtils.unescapeJson(s);
//		System.out.println("visitString after " + s);
		// if(s!=null) s = s.substring(1, s.length()-1);
		return new FutureInstructionValue<JSON>(s);
	}



	@Override
	public FutureInstructionValue<JSON> visitPnum(PnumContext ctx) {
		if (ctx.INTEGER() == null) {
			Double d = new Double(ctx.FLOAT().getText());
			return new FutureInstructionValue<JSON>(d);
		} else {
			Long l = new Long(ctx.INTEGER().getText());
			return new FutureInstructionValue<JSON>(l);
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitVariable(VariableContext ctx) {
		int n = ctx.getChildCount();
		String name = null;
		Iterator<IdentContext> ids = ctx.ident().iterator();
		if (n == 4) {
			name = ids.next().getText();
			name = name + "." + ids.next().getText();
		}
		// n == 2
		else if (ids.hasNext()) {
			name = ids.next().getText();
		} else {
			TerminalNode ii = ctx.INTEGER();
			if (ii != null) {
				name = ii.getText();
			} else {
				ParseTree pt = ctx.getChild(1);
				name = pt.getText();
			}
		}
		return new FutureInstructionValue<JSON>(variable(getSource(ctx), name));
	}

	@Override
	public FutureInstructionValue<JSON> visitId(IdContext ctx) {
		IdentContext ic = ctx.ident();
		String c;
		if (ic != null) {
			c = visitIdent(ic).string;
			IdContext idc = ctx.id();
			if (idc != null) {
				String a = visitId(idc).string;
				StringBuilder builder = new StringBuilder(a).append('.').append(c);
				return new FutureInstructionValue<>(builder.toString());
			}
			return new FutureInstructionValue<>(c);
		}
		return new FutureInstructionValue<>(visitId(ctx.id()).string);

		/*
		 * String prefix = ctx.getChild(0).getText(); String dd =
		 * visitId(ctx.id()).string; StringBuilder builder = new
		 * StringBuilder(prefix).append(dd); return new
		 * InstructionFutureValue<>(builder.toString());
		 */
	}

	@Override
	public FutureInstructionValue<JSON> visitIdent(IdentContext ctx) {
		return new FutureInstructionValue<JSON>(ctx.getText());
	}

	@Override
	public FutureInstructionValue<JSON> visitNumber(NumberContext ctx) {
		Number num = visitPnum(ctx.pnum()).number;
		int n = ctx.getChildCount();
		if (n > 1) {
			if (num instanceof Long) {
				num = -(Long) num;
			} else {
				num = -(Double) num;
			}
		}
		return new FutureInstructionValue<JSON>(number(num, builder, getSource(ctx)));
	}

	@Override
	public FutureInstructionValue<JSON> visitJpath(JpathContext ctx) {
		return visitRe_expr(ctx.re_expr());
	}

	@Override
	public FutureInstructionValue<JSON> visitRe_expr(Re_exprContext ctx) {

		Tern_exprContext tc = ctx.tern_expr();
		if (tc != null)
			return visitTern_expr(tc);
		FutureInstructionValue<JSON> a = visitRe_expr(ctx.re_expr());
		String s = visitString(ctx.string()).string;
		SourceInfo si = getSource(ctx);
		si.name="rematch";
		return new FutureInstructionValue<>(reMatch(si, s, a.inst));
	}

	@Override
	public FutureInstructionValue<JSON> visitTern_expr(Tern_exprContext ctx) {
		Or_exprContext oc = ctx.or_expr();
		if (oc != null)
			return visitOr_expr(oc);
		Tern_exprContext tc = ctx.tern_expr();
		Iterator<ValueContext> vit = ctx.value().iterator();
		ValueContext va = vit.next();
		ValueContext vb = vit.next();
		return new FutureInstructionValue<>(
				conditional(visitTern_expr(tc).inst, visitValue(va).inst, visitValue(vb).inst, getSource(ctx)));
	}

	@Override
	public FutureInstructionValue<JSON> visitOr_expr(Or_exprContext ctx) {
		FutureInstructionValue<JSON> a = visitAnd_expr(ctx.and_expr());
		Or_exprContext c = ctx.or_expr();
		if (c != null) {
	//		final boolean or = ctx.getChild(1).getText().equals("or");
			return new FutureInstructionValue<JSON>(
					ContextualInstructionFactory.trueOr(SourceInfo.internal("<or>"), visitOr_expr(c).inst, a.inst));
		} else {
			return a;
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitAnd_expr(And_exprContext ctx) {
		FutureInstructionValue<JSON> a = visitEq_expr(ctx.eq_expr());
		And_exprContext c = ctx.and_expr();
		if (c != null) {
			return new FutureInstructionValue<JSON>(
					dyadic(getSource(ctx), visitAnd_expr(c).inst, a.inst, new DyadicAsyncFunction<JSON>() {
						@Override
						public JSON invoke(AsyncExecutionContext<JSON> eng, JSON a, JSON b) {
							if (a.getType() == JSONType.LIST) {
								if (b.isTrue())
									((JList) a).add(b);
							}
							return builder.value(a.isTrue() && b.isTrue());
						}
					}, false));
		} else {
			return a;
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitEq_expr(Eq_exprContext ctx) {
		FutureInstructionValue<JSON> a = visitRel_expr(ctx.rel_expr());
		Eq_exprContext c = ctx.eq_expr();

		if (c != null) {
			final boolean inv = ctx.getChild(1).getText().equals("!=");
			return new FutureInstructionValue<JSON>(
					dyadic(getSource(ctx), visitEq_expr(c).inst, a.inst, new DyadicAsyncFunction<JSON>() {
						@Override
						public JSON invoke(AsyncExecutionContext<JSON> eng, JSON a, JSON b) {
							boolean e = a.equals(b);
							return builder.value(inv ^ e);
						}
					}, false));

		} else {
			return a;
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitNot_expr(Not_exprContext ctx) {
		FutureInstructionValue<JSON> a = visitRel_expr(ctx.rel_expr());
		if(a == null) {
		Not_exprContext c = ctx.not_expr();
			final boolean inv = ctx.getChild(1).getText().equals("!=");
			return new FutureInstructionValue<JSON>(
					ContextualInstructionFactory.trueNot(SourceInfo.internal("not"), visitNot_expr(c).inst));
		} else {
			return a;
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitRel_expr(Rel_exprContext ctx) {
		FutureInstructionValue<JSON> a = visitAdd_expr(ctx.add_expr());
		Rel_exprContext c = ctx.rel_expr();
		if (c != null) {
			DyadicAsyncFunction<JSON> df = null;
			switch (ctx.getChild(1).getText()) {
			case "<":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compareTo(r) < 0);
					}
				};
				break;
			case ">":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compareTo(r) > 0);
					}
				};
				break;
			case "<=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compareTo(r) <= 0);
					}
				};
				break;
			case ">=":
				df = new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON l, JSON r) {
						return builder.value(l.compareTo(r) >= 0);
					}
				};
				break;
			}
			return new FutureInstructionValue<JSON>(dyadic(getSource(ctx), visitRel_expr(c).inst, a.inst, df, false));
		} else {
			return a;
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitAdd_expr(Add_exprContext ctx) {
		FutureInstructionValue<JSON> a = visitMul_expr(ctx.mul_expr());
		Add_exprContext c = ctx.add_expr();
		if (c != null) {
			FutureInstructionValue<JSON> bv = visitAdd_expr(c);
			String sop = ctx.getChild(1).getText();
			switch (sop) {
			case "+":
				return new FutureInstructionValue<JSON>(addInstruction(getSource(ctx), bv.inst, a.inst));
			case "-":
				return new FutureInstructionValue<>(subInstruction(getSource(ctx), bv.inst, a.inst));
			}
		}
		return a;

	}

	@Override
	public FutureInstructionValue<JSON> visitMul_expr(Mul_exprContext ctx) {
		FutureInstructionValue<JSON> a = visitUnary_expr(ctx.unary_expr());
		Mul_exprContext c = ctx.mul_expr();
		if (c != null) {
			FutureInstructionValue<JSON> bv = visitMul_expr(c);
			String sop = ctx.getChild(1).getText();
			switch (sop) {
			case "*":
				return new FutureInstructionValue<>(mulInstruction(getSource(ctx), bv.inst, a.inst));
			case "div":
				return new FutureInstructionValue<>(divInstruction(getSource(ctx), bv.inst, a.inst));
			case "%":
				return new FutureInstructionValue<>(modInstruction(getSource(ctx), bv.inst, a.inst));
			}
		}
		return a;
	}

	@Override
	public FutureInstructionValue<JSON> visitUnary_expr(Unary_exprContext ctx) {
		Union_exprContext uc = ctx.union_expr();
		if (uc != null)
			return visitUnion_expr(uc);
		FutureInstructionValue<JSON> a = visitUnary_expr(ctx.unary_expr());
		return new FutureInstructionValue<JSON>(negate(getSource(ctx), a.inst));
	}

	@Override
	public FutureInstructionValue<JSON> visitUnion_expr(Union_exprContext ctx) {
		List<Filter_pathContext> fps = ctx.filter_path();
		if (fps.size() == 1) {
			return visitFilter_path(fps.get(0));
		} else {
			final List<FutureInstruction<JSON>> seq = new ArrayList<>();
			for (Filter_pathContext fp : fps) {
				seq.add(visitFilter_path(fp).inst);
			}
			return new FutureInstructionValue<>(union(getSource(ctx), seq));
		}
	}

	@Override
	public FutureInstructionValue<JSON> visitFilter_path(Filter_pathContext ctx) {
		PathContext pc = ctx.path();
		if (pc != null)
			return visitPath(pc);
		FutureInstructionValue<JSON> a = visitFilter_path(ctx.filter_path());
		FutureInstructionValue<JSON> b = visitValue(ctx.value());
		return new FutureInstructionValue<>(dereference(getSource(ctx), a.inst, b.linst));
	}

	@Override
	public FutureInstructionValue<JSON> visitPath(PathContext ctx) {
		Abs_pathContext ac = ctx.abs_path();
		return visitAbs_path(ac);
	}

	@Override
	public FutureInstructionValue<JSON> visitAbs_path(Abs_pathContext ctx) {
		final FutureInstructionValue<JSON> a = visitRel_path(ctx.rel_path());
		if (1 < ctx.getChildCount()) {
			return new FutureInstructionValue<>(abspath(getSource(ctx), a.inst));
		}
		return a;
	}

	@Override
	public FutureInstructionValue<JSON> visitRel_path(Rel_pathContext ctx) {
		final FutureInstructionValue<JSON> a = visitPathelement(ctx.pathelement());
		Rel_pathContext rp = ctx.rel_path();
		if (rp != null) {
			final FutureInstructionValue<JSON> c = visitRel_path(rp);
			return new FutureInstructionValue<JSON>(relpath(getSource(ctx), c.inst, a.inst));
		}
		return a;
	}
	@Override
	public FutureInstructionValue<JSON> visitFuncprefix(FuncprefixContext ctx) {
		List<IdentContext> ids = ctx.ident();
		if(ids.size() == 0) {
			TerminalNode ii = ctx.INTEGER();
			return new FutureInstructionValue<JSON>(ii.getText());
		}
		if(ids.size() == 1) {
			return new FutureInstructionValue<JSON>(ids.get(0).getText());
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(IdentContext idc: ids) {
			if(first) first = false;
			else sb.append('.');
			sb.append(idc.getText());
		}
		return new FutureInstructionValue<JSON>(sb.toString());
	}
	@Override
	public FutureInstructionValue<JSON> visitFunctionCall(FunctionCallContext ctx) {
		final FutureInstructionValue<JSON> a = visitFuncprefix(ctx.funcprefix());
		List<FutureInstruction<JSON>> iargs = new ArrayList<>();
		for(ValueContext vc : ctx.value()) {
			iargs.add(visitValue(vc).inst);
		}
		return new FutureInstructionValue<JSON>(functionCall(getSource(ctx), a.string, iargs));
	}
	@Override
	public FutureInstructionValue<JSON> visitPathelement(PathelementContext ctx) {
		PathstepContext psc = ctx.pathstep();
		if (psc != null) {
			return visitPathstep(psc);
		}
		PathelementContext pc = ctx.pathelement();
		FutureInstructionValue<JSON> pif = visitPathelement(pc);
		FutureInstructionValue<JSON> vif = visitPathindex(ctx.pathindex());

		return new FutureInstructionValue<JSON>(dereference(getSource(ctx), pif.inst, vif.linst));
	}

	@Override
	public FutureInstructionValue<JSON> visitPathstep(PathstepContext ctx) {
		ObjectContext oc = ctx.object();
		if (oc != null) return visitObject(oc);

		ArrayContext ac = ctx.array();
		if (ac != null) return visitArray(ac);
		
		ValueContext xvc = ctx.value();
		if (xvc != null) return visitValue(xvc);

		NumberContext nc = ctx.number();
		if (nc != null)	return visitNumber(nc);

		StringContext sc = ctx.string();
		if (sc != null) {
			FutureInstructionValue<JSON> dd = visitString(sc);
			dd.inst = value(dd.string, builder, getSource(ctx));
			return dd;
		}
		
		VariableContext vc = ctx.variable();
		if (vc != null)	return visitVariable(vc);

		FunctionCallContext fc = ctx.functionCall();
		if (fc != null)	return visitFunctionCall(fc);

		IdContext ic = ctx.id();
		if (ic != null) {
			String id = visitId(ic).string;
			return new FutureInstructionValue<>(get(getSource(ctx), id));
		}


		RecursContext rc = ctx.recurs();
		if (rc != null) return visitRecurs(rc);


		String t = ctx.getText();
		{
			switch (t) {
			case "true":
				return new FutureInstructionValue<>(value(true, builder, getSource(ctx)));
			case "false":
				return new FutureInstructionValue<>(value(false, builder, getSource(ctx)));
			case "null":
				return new FutureInstructionValue<>(value(builder, getSource(ctx)));
			case "*":
				return new FutureInstructionValue<>(stepChildren(getSource(ctx)));
			case ".":
				return new FutureInstructionValue<>(stepSelf(getSource(ctx)));
			case "..":
				return new FutureInstructionValue<>(stepParent(getSource(ctx)));

			}
		}
		throw new RuntimeException("token " + t + " is not implemented in pathstep");
	}

	@Override
	public FutureInstructionValue<JSON> visitRecurs(RecursContext ctx) {
		String t = ctx.getText();
		switch (t) {
		case "**":
			return new FutureInstructionValue<>(recursDown(getSource(ctx)));
		case "...":
			return new FutureInstructionValue<>(recursUp(getSource(ctx)));
		}
		throw new RuntimeException("token " + t + " is not implemented in recurs");
	}

	/*
	 * @Override public InstructionFutureValue<JSON> visitJstring(JstringContext
	 * ctx) { StringContext sc = ctx.string(); if (sc != null) return new
	 * InstructionFutureValue<JSON>(string(visitString(sc).string)); StrcContext
	 * stc = ctx.strc(); return visitStrc(stc); }
	 * 
	 * @Override public InstructionFutureValue<JSON> visitStrc(StrcContext ctx)
	 * { JtlContext jc = ctx.jtl(); if (jc != null) { return new
	 * InstructionFutureValue<>(tostr(visitJtl(jc).inst)); } List<StrcContext>
	 * strc = ctx.strc(); if (strc != null && strc.size() > 0) {
	 * List<InstructionFuture<JSON>> ii = new ArrayList<>(strc.size()); for
	 * (StrcContext sc : strc) { ii.add(visitStrc(sc).inst); } return new
	 * InstructionFutureValue<>(strc(ii)); } return new
	 * InstructionFutureValue<>(value(ctx.getText())); }
	 */
	protected SourceInfo getSource(ParserRuleContext ctx, String name) {
		return JtlCompiler.getSource(source, ctx, name);
	}

	protected SourceInfo getSource(ParserRuleContext ctx) {
		return JtlCompiler.getSource(source, ctx, null);
	}

	@Override
	public FutureInstructionValue<JSON> visitPathindex(PathindexContext ctx) {
		return visitIndexlist2(ctx.indexlist2());
	}

	@Override
	public FutureInstructionValue<JSON> visitIndexlist2(Indexlist2Context ctx) {
		List<FutureInstruction<JSON>> elements = new ArrayList<>();
		// List<IndexlContext> l= ctx.indexl();
		for (IndexlContext dxlc : ctx.indexl()) {
			elements.add(visitIndexl(dxlc).inst);

		}

		return new FutureInstructionValue<>(elements);
	}

	@Override
	public FutureInstructionValue<JSON> visitIndexlist(IndexlistContext ctx) {
		List<FutureInstruction<JSON>> elements = new ArrayList<>();
		// List<IndexlContext> l= ctx.indexl();
		for (IndexlContext dxlc : ctx.indexl()) {
			elements.add(visitIndexl(dxlc).inst);

		}

		return new FutureInstructionValue<>(array(elements, getSource(ctx)));
	}

	@Override
	public FutureInstructionValue<JSON> visitIndexl(IndexlContext ctx) {
		List<ValueContext> cl = ctx.value();
		FutureInstruction<JSON> inst = visitValue(cl.get(0)).inst;
		if (cl.size() > 1) {
			return new FutureInstructionValue<>(new RangeInstruction(inst, visitValue(cl.get(1)).inst, getSource(ctx)));
		} else {
			return new FutureInstructionValue<>(inst);
		}
	}

}
