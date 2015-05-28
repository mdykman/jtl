package org.dykman.jtl.core.parser;

import java.util.Collection;
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

import org.antlr.v4.runtime.tree.TerminalNode;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONBuilder;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.Pair;

public class DataVisitor extends jtlBaseVisitor<DataValue<JSON>> {

	JSONBuilder builder;
	public DataVisitor(JSONBuilder builder) {
		this.builder = builder;
	}
	@Override
	public DataValue<JSON> visitJtl(JtlContext ctx) {
		JsonContext c = ctx.json();
		DataValue<JSON> v = visitJson(c);
		v.value.lock();
		return v;
	}
	@Override
	public DataValue<JSON> visitJson(JsonContext ctx) {
		return visitValue(ctx.value());
//		return super.visitJson(ctx);
	}
	@Override
	public DataValue<JSON> visitObject(ObjectContext ctx) {
		int cap = ctx.getChildCount();
		JSONObject obj = builder.object(null,cap);
		for(PairContext p: ctx.pair()) {
			DataValue<JSON> v = visitPair(p);
			obj.put(v.pair.f,v.pair.s);
		}
		return new DataValue<JSON>(obj);
	}
	
	@Override
	public DataValue<JSON> visitPair(PairContext ctx) {
		String k;
		IdContext id = ctx.id();
		if(id!=null) {
			DataValue<JSON> v = visitId(id);
			k = v.str;
		} else {
			DataValue<JSON> v = visitString(ctx.string());
			k = v.str;
		}
		DataValue<JSON> v = visitValue(ctx.value());
		v.value.setName(k);
		return new DataValue<JSON>(new Pair<String, JSON>(k,v.value));
	}
	
	@Override
	public DataValue<JSON> visitArray(ArrayContext ctx) {
		int cc = ctx.getChildCount();
//		Collection<JSON> c = builder.collection(cc);
		JSONArray arr = builder.array(null);
		for(ValueContext v: ctx.value()) {
			DataValue<JSON> dv = visitValue(v);
			arr.add(dv.value);
		}
		return new DataValue<JSON>(arr);
	}
	@Override
	public DataValue<JSON> visitValue(ValueContext ctx) {
		ValueContext vc = ctx.value();
		if(vc!=null) return visitValue(vc);
		JpathContext jc = ctx.jpath();
		if(jc!=null) return visitJpath(jc);
		ObjectContext oc = ctx.object();
		if(oc!=null) return visitObject(oc);
		ArrayContext ac = ctx.array();
		if(ac!=null) return visitArray(ac);
//		DataValue<JSON> v = super.visitValue(ctx);
// I should trap non-data nodes seen here and raise input exception
//		return v;
		
		// TODO:: thrown not implemented expetion
		return null;
	}
	@Override
	public DataValue<JSON> visitS_expr(S_exprContext ctx) {
		return visitJson(ctx.json());
//		return super.visitS_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitJpath(JpathContext ctx) {
		return visitTern_expr(ctx.tern_expr());
	}
	@Override
	public DataValue<JSON> visitOr_expr(Or_exprContext ctx) {
		return visitAnd_expr(ctx.and_expr());
//		return super.visitOr_expr(ctx);ctx.
	}
	@Override
	public DataValue<JSON> visitAnd_expr(And_exprContext ctx) {
		return visitEq_expr(ctx.eq_expr());
//		return super.visitAnd_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitEq_expr(Eq_exprContext ctx) {
		return visitRel_expr(ctx.rel_expr());
//		return super.visitEq_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitRel_expr(Rel_exprContext ctx) {
		return visitAdd_expr(ctx.add_expr());
//		return super.visitRel_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitAdd_expr(Add_exprContext ctx) {
		return visitMul_expr(ctx.mul_expr());
//		return super.visitAdd_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitMul_expr(Mul_exprContext ctx) {
		return visitUnary_expr(ctx.unary_expr());
//		return super.visitMul_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitUnary_expr(Unary_exprContext ctx) {
		return visitUnion_expr(ctx.union_expr());
//		return super.visitUnary_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitUnion_expr(Union_exprContext ctx) {
		if(ctx.union_expr()!=null) throw new RuntimeException("illegal union expression in data");
		return visitFilter_path(ctx.filter_path());
//		return super.visitUnion_expr(ctx);
	}
	@Override
	public DataValue<JSON> visitFilter_path(Filter_pathContext ctx) {
		if(ctx.filter_path()!=null) throw new RuntimeException("illegal filter path in data");
		return visitPath(ctx.path());
		
//		return super.visitFilter_path(ctx);
	}
	@Override
	public DataValue<JSON> visitPath(PathContext ctx) {
		
		return visitAbs_path(ctx.abs_path());
//		return super.visitPath(ctx);
	}
	@Override
	public DataValue<JSON> visitAbs_path(Abs_pathContext ctx) {
	//	if(ctx.!=null) throw new RuntimeException("illegal relative path");
		return visitRel_path(ctx.rel_path());
//		return super.visitAbs_path(ctx);
		
	}
	@Override
	public DataValue<JSON> visitRel_path(Rel_pathContext ctx) {
		if(ctx.rel_path()!=null) throw new RuntimeException("illegal relative path in data");
		return visitPathelement(ctx.pathelement());
//		return super.visitRel_path(ctx);
	}
	@Override
	public DataValue<JSON> visitPathelement(PathelementContext ctx) {
		return visitPathstep(ctx.pathstep());
//		return super.visitPathelement(ctx);
	}
	@Override
	public DataValue<JSON> visitPathstep(PathstepContext ctx) {
		StringContext sc = ctx.string();
		if(sc!=null) {
			return visitString(sc);
		}
		NumberContext nc = ctx.number();
		if(nc!=null) {
			return visitNumber(nc);
		}
		throw new RuntimeException("not implemented");
//		ctx.recurs();
//		ctx.func();
//		ctx.id();
//		ctx.getText();
//		return super.visitPathstep(ctx);
//		return visitRecurs(ctx.recurs());
		// TODO Auto-generated method stub
//		return super.visitPathstep(ctx);
	}
	@Override
	public DataValue<JSON> visitRecurs(RecursContext ctx) {
		return super.visitRecurs(ctx);
	}
	@Override
	public DataValue<JSON> visitFunc(FuncContext ctx) {
		return super.visitFunc(ctx);
	}
	@Override
	public DataValue<JSON> visitVariable(VariableContext ctx) {
		return super.visitVariable(ctx);
	}
	@Override
	public DataValue<JSON> visitNumber(NumberContext ctx) {
		TerminalNode tn = ctx.INTEGER();
		if(tn!=null) {
			return new DataValue<JSON>(builder.value(Long.parseLong(tn.getText())));
		}
		tn = ctx.FLOAT();
		return new DataValue<JSON>(builder.value(Double.parseDouble(tn.getText())));
	}
	@Override
	public DataValue<JSON> visitId(IdContext ctx) {
		return new DataValue<JSON>(ctx.ID().getText());
	}
	@Override
	public DataValue<JSON> visitString(StringContext ctx) {
		String k = ctx.STRING().getText();
		k=k.substring(1,k.length()-1);
		return new DataValue<JSON>(builder.value(k));
//		return super.visitString(ctx);
	}

}
