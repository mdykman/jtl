package org.dykman.jtl.json;



import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.dykman.jtl.Pair;
import org.dykman.jtl.jsonBaseVisitor;
import org.dykman.jtl.jsonParser.ArrayContext;
import org.dykman.jtl.jsonParser.JsonContext;
import org.dykman.jtl.jsonParser.KeyContext;
import org.dykman.jtl.jsonParser.NumberContext;
import org.dykman.jtl.jsonParser.ObjectContext;
import org.dykman.jtl.jsonParser.PairContext;
import org.dykman.jtl.jsonParser.StringContext;
import org.dykman.jtl.jsonParser.ValueContext;

public class DataVisitor extends jsonBaseVisitor<DataValue<JSON>> {

	JSONBuilder builder;
	public DataVisitor(JSONBuilder builder) {
		this.builder = builder;
	}
	@Override
	public DataValue<JSON> visitJson(JsonContext ctx) {
		return visitValue(ctx.value());
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
		KeyContext id = ctx.key();
      DataValue<JSON> v = visitKey(id);
      k = v.str;
		v = visitValue(ctx.value());
		v.value.setName(k);
		return new DataValue<JSON>(new Pair<String, JSON>(k,v.value));
	}
	
	@Override
	public DataValue<JSON> visitArray(ArrayContext ctx) {
		int cc = ctx.getChildCount();
		JSONArray arr = builder.array(null);
		for(ValueContext v: ctx.value()) {
			DataValue<JSON> dv = visitValue(v);
			arr.add(dv.value);
		}
		return new DataValue<JSON>(arr);
	}
	@Override
	public DataValue<JSON> visitValue(ValueContext ctx) {
		ObjectContext oc = ctx.object();
		if(oc!=null) return visitObject(oc);
		ArrayContext ac = ctx.array();
		if(ac!=null) return visitArray(ac);
		NumberContext nc = ctx.number();
		if(nc!=null) return visitNumber(nc);
		StringContext sc = ctx.string();
		if(sc!=null) return  new DataValue(builder.value(visitString(sc).str));
		
		ParseTree pt = ctx.getChild(0);
		switch(pt.getText()) {
		case "true": return new DataValue<JSON>(builder.value(true));
		case "false": return new DataValue<JSON>(builder.value(false));
		case "null": return new DataValue<JSON>(builder.value());
		default: return new  DataValue<JSON>(builder.value(pt.getText()));
		}
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
	public DataValue<JSON> visitKey(KeyContext ctx) {
		StringContext sc = ctx.string();
		if(sc!=null) {
			return new DataValue<JSON>(visitString(sc).str);
		}
		return new DataValue<JSON>(ctx.ID().getText());
	}

	@Override
	public DataValue<JSON> visitString(StringContext ctx) {
		TerminalNode tn = ctx.STRING();
		if(tn==null) {
			tn = ctx.SSTRING();
		}
		String k = tn.getText();
		k=k.substring(1,k.length()-1);
		return new DataValue<JSON>(k);
		
	}

}
