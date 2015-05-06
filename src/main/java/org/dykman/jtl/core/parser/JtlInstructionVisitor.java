package org.dykman.jtl.core.parser;

import java.util.ArrayList;
import java.util.List;

import main.antlr.jtlBaseVisitor;
import main.antlr.jtlParser.ArrayContext;
import main.antlr.jtlParser.FuncContext;
import main.antlr.jtlParser.IdContext;
import main.antlr.jtlParser.JpathContext;
import main.antlr.jtlParser.JsonContext;
import main.antlr.jtlParser.JtlContext;
import main.antlr.jtlParser.NumberContext;
import main.antlr.jtlParser.ObjectContext;
import main.antlr.jtlParser.PairContext;
import main.antlr.jtlParser.S_exprContext;
import main.antlr.jtlParser.StringContext;
import main.antlr.jtlParser.ValueContext;
import main.antlr.jtlParser.VariableContext;

import org.antlr.v4.runtime.RuleContext;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.engine.Instruction;
import org.dykman.jtl.core.engine.InstructionFactory;

public class JtlInstructionVisitor extends jtlBaseVisitor<InstructionValue<JSON>> {

	@Override
	public InstructionValue<JSON> visitJtl(JtlContext ctx) {
		return super.visitJtl(ctx);
	}

	@Override
	public InstructionValue<JSON> visitJson(JsonContext ctx) {
		return super.visitJson(ctx);
	}

	@Override
	public InstructionValue<JSON> visitObject(ObjectContext ctx) {
		final List<InstructionValue<JSON>> list = new ArrayList<>();
			for(PairContext p : ctx.pair()) {
				list.add(visitPair(p));
			}
		return new InstructionValue<JSON>(
				InstructionFactory.object(list));
	}

	@Override
	public InstructionValue<JSON> visitPair(PairContext ctx) {
		ValueContext c = ctx.value();
	
		InstructionValue<JSON> k = null;
		{ IdContext id = ctx.id(); if(id!=null) k = visitId(id); }
		{ StringContext id = ctx.string(); if(id!=null) k = visitString(id); }
		JSON kj = k.inst.call(null, null, null);
		return new InstructionValue<JSON>(kj.toString(),visitValue(c).inst);
	}

	@Override
	public InstructionValue<JSON> visitArray(ArrayContext ctx) {
			List<Instruction<JSON>> ll = new ArrayList<>();
			for(ValueContext vc : ctx.value()) {
				ll.add(visitValue(vc).inst);
			}
			return new InstructionValue<JSON>(InstructionFactory.array(ll));
	}

	@Override
	public InstructionValue<JSON> visitValue(ValueContext ctx) {
		{ ObjectContext c = ctx.object();     if(c != null) return visitObject(c); }
		{ ArrayContext c = ctx.array();       if(c != null) return visitArray(c); }
		{ JpathContext c = ctx.jpath();         if(c != null) return visitJpath(c); }
		
		String t = ctx.getText();
		return new InstructionValue<>(t.equalsIgnoreCase("true") ? 
			InstructionFactory.bool(true ): t.equalsIgnoreCase("null") ? 
				InstructionFactory.nil() : InstructionFactory.bool(false));
	}

	@Override
	public InstructionValue<JSON> visitString(StringContext ctx) {	
//		InstructionValue<JSON> iv = super.visitString(ctx);
		String t = ctx.getText();
		t = t.substring(1).substring(0,t.length()-1);
		return new InstructionValue<>(InstructionFactory.string(t));
	}

	@Override
	public InstructionValue<JSON> visitS_expr(S_exprContext ctx) {
		return super.visitS_expr(ctx);
	}

	@Override
	public InstructionValue<JSON> visitFunc(FuncContext ctx) {
		List<Instruction<JSON>> farg = new ArrayList<>();
		for(JsonContext c: ctx.json()) {
			farg.add(visitJson(c).inst);
		}
		String n = ctx.getChild(0).getText();
		return new InstructionValue<>(InstructionFactory.function(n,farg));
	}

	@Override
	public InstructionValue<JSON> visitVariable(VariableContext ctx) {
		return super.visitVariable(ctx);
	}

	@Override
	public InstructionValue<JSON> visitId(IdContext ctx) {
		super.visitId(ctx);
		return new InstructionValue<>(InstructionFactory.string(ctx.str));
	}

	@Override
	public InstructionValue<JSON> visitNumber(NumberContext ctx) {
		super.visitNumber(ctx);
		return new InstructionValue<>(InstructionFactory.number(ctx.num));
	}

}
