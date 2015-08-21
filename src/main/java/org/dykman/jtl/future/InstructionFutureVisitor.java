package org.dykman.jtl.future;

import static org.dykman.jtl.future.InstructionFutureFactory.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.JtlCompiler;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.jtlBaseVisitor;
import org.dykman.jtl.jtlParser.Abs_pathContext;
import org.dykman.jtl.jtlParser.Add_exprContext;
import org.dykman.jtl.jtlParser.And_exprContext;
import org.dykman.jtl.jtlParser.ArrayContext;
import org.dykman.jtl.jtlParser.Eq_exprContext;
import org.dykman.jtl.jtlParser.FfContext;
import org.dykman.jtl.jtlParser.Filter_pathContext;
import org.dykman.jtl.jtlParser.FuncContext;
import org.dykman.jtl.jtlParser.FuncderefContext;
import org.dykman.jtl.jtlParser.IdContext;
import org.dykman.jtl.jtlParser.IdentContext;
import org.dykman.jtl.jtlParser.IndexlContext;
import org.dykman.jtl.jtlParser.IndexlistContext;
import org.dykman.jtl.jtlParser.JpathContext;
import org.dykman.jtl.jtlParser.JsonContext;
import org.dykman.jtl.jtlParser.JtlContext;
import org.dykman.jtl.jtlParser.KeyContext;
import org.dykman.jtl.jtlParser.Mul_exprContext;
import org.dykman.jtl.jtlParser.NumberContext;
import org.dykman.jtl.jtlParser.ObjectContext;
import org.dykman.jtl.jtlParser.Or_exprContext;
import org.dykman.jtl.jtlParser.PairContext;
import org.dykman.jtl.jtlParser.PathContext;
import org.dykman.jtl.jtlParser.PathelementContext;
import org.dykman.jtl.jtlParser.PathindexContext;
import org.dykman.jtl.jtlParser.PathstepContext;
import org.dykman.jtl.jtlParser.PnumContext;
import org.dykman.jtl.jtlParser.Re_exprContext;
import org.dykman.jtl.jtlParser.RecursContext;
import org.dykman.jtl.jtlParser.Rel_exprContext;
import org.dykman.jtl.jtlParser.Rel_pathContext;
import org.dykman.jtl.jtlParser.StringContext;
import org.dykman.jtl.jtlParser.Tern_exprContext;
import org.dykman.jtl.jtlParser.Unary_exprContext;
import org.dykman.jtl.jtlParser.Union_exprContext;
import org.dykman.jtl.jtlParser.ValueContext;
import org.dykman.jtl.jtlParser.VariableContext;
import org.dykman.jtl.json.Frame;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSON.JSONType;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;

public class InstructionFutureVisitor extends jtlBaseVisitor<InstructionFutureValue<JSON>> {

	final JSONBuilder builder;
	volatile boolean imported = false;
	final String source;
	public InstructionFutureVisitor(String source,JSONBuilder builder) {
		this(source,builder,false);
	}
   public InstructionFutureVisitor(JSONBuilder builder) {
      this(null,builder,false);
   }
	public InstructionFutureVisitor(String source,JSONBuilder builder, boolean imported) {
		this.builder = builder;
		this.imported = imported;
		this.source = source;
	}

	@Override
	public InstructionFutureValue<JSON> visitJtl(JtlContext ctx) {
		ValueContext jc = ctx.value();
		return visitValue(jc);
	}

	protected void sealResult(JSON j) {
		JSONType type = j.getType();
		switch (type) {
			case FRAME:
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
			} break;
			default:
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitJson(JsonContext ctx) {
		return visitValue(ctx.value());
	}

	@Override
	public InstructionFutureValue<JSON> visitObject(ObjectContext ctx) {
		List<Pair<String, InstructionFuture<JSON>>> ins = new ArrayList<>(ctx.getChildCount());
		InstructionFutureValue<JSON> pp;
		for (PairContext p : ctx.pair()) {
			pp = visitPair(p);
			ins.add(new Pair<String, InstructionFuture<JSON>>(pp.ninst.f, pp.ninst.s));
		}

		try {
			boolean _import = false; 
			if(imported) {
				imported = false;
				_import = true;
			}
			return new InstructionFutureValue<JSON>(object(getSource(ctx),ins,_import));
		} catch (ExecutionException e) {
			return new InstructionFutureValue<JSON>(value("ExecutionException during visitObject: "
				+ e.getLocalizedMessage(),builder,getSource(ctx)));
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitPair(PairContext ctx) {
		InstructionFutureValue<JSON> k = null;
		KeyContext id = ctx.key();
		if (id != null) {
			k = visitKey(id);
		}
		String ks = k.string;

		InstructionFutureValue<JSON> v = visitValue(ctx.value());
		return new InstructionFutureValue<JSON>(ks, v.inst);
	}

	@Override 
	public InstructionFutureValue<JSON> visitKey(KeyContext ctx) {
	   IdentContext ic = ctx.ident();
	   if(ic!=null) {
	      String s = visitIdent(ic).string;
	      if(ctx.getChildCount() > 1) {
	         return new InstructionFutureValue<JSON>(ctx.getChild(0).getText() + s);
	      }
         return new InstructionFutureValue<JSON>(s);
	   }
	   return new InstructionFutureValue<JSON>(visitString(ctx.string()).string);
	}

	@Override
	public InstructionFutureValue<JSON> visitArray(ArrayContext ctx) {
		List<InstructionFuture<JSON>> ins = new ArrayList<>(ctx.getChildCount());
		for (ValueContext vc : ctx.value()) {
			ins.add(visitValue(vc).inst);
		}
		return new InstructionFutureValue<JSON>(array(ins,getSource(ctx)));
	}

	@Override
	public InstructionFutureValue<JSON> visitValue(ValueContext ctx) {
		JpathContext jc = ctx.jpath();
			return visitJpath(jc);
//		throw new RuntimeException("unknown value type");
	}

	@Override
		public InstructionFutureValue<JSON> visitString(StringContext ctx) {
//		TerminalNode tn = ctx.StringLiteral();
		TerminalNode tn = ctx.STRING();
		String s = null;
		if(tn != null) {
			s = tn.getText();
		} else {
			tn = ctx.SSTRING();
			s = tn.getText();
		}
		return new InstructionFutureValue<JSON>(s.substring(1, s.length()-1));
	}

	@Override
   public InstructionFutureValue<JSON> visitFf(FfContext ctx) {
      // TODO Auto-generated method stub
      return super.visitFf(ctx);
   }
	
   @Override
   public InstructionFutureValue<JSON> visitFuncderef(FuncderefContext ctx) {
      String name = ctx.getChild(1).getText();
      List<InstructionFuture<JSON>> ins = new ArrayList<>(ctx.getChildCount());
      for (ValueContext jc : ctx.value()) {
         InstructionFutureValue<JSON> vv = visitValue(jc);
         ins.add(vv.inst);
      }
      return new InstructionFutureValue<JSON>(activate(getSource(ctx),name,ins));
   }
   
   @Override
	public InstructionFutureValue<JSON> visitFunc(FuncContext ctx) {
	   FfContext fc = ctx.ff();
	   String name = fc.getChild(0).getText();
	   IdentContext ic = ctx.ident();
	   if(ic!=null) {
	      name = visitIdent(ic).string + '.' + name;
	   }
      List<InstructionFuture<JSON>> ins = new ArrayList<>(ctx.getChildCount());
      for (ValueContext jc : fc.value()) {
         InstructionFutureValue<JSON> vv = visitValue(jc);
         ins.add(vv.inst);
      }
      return new InstructionFutureValue<JSON>(function(getSource(ctx),name, ins));
	}

   @Override
   public InstructionFutureValue<JSON> visitPnum(PnumContext ctx) {
      if(ctx.INTEGER() ==null) {
         Double d = new Double(ctx.FLOAT().getText());
         return new InstructionFutureValue<JSON>(d);
      } else {
         Long l = new Long(ctx.INTEGER().getText());
         return new InstructionFutureValue<JSON>(l);
      }
  }

   @Override
	public InstructionFutureValue<JSON> visitVariable(VariableContext ctx) {
	   List<IdentContext> iit = ctx.ident();
	   int n = iit.size();
	   String name = null;
	   if(n == 0) {
	      name = ctx.getChild(1).getText();
	   } else {
	      name = visitIdent(iit.get(0)).string;
	   }
	   if(n ==4 ) {
	     name = visitIdent(iit.get(0)).string + '.' + name;
	   }
	   List<InstructionFuture<JSON>> ins = new ArrayList<>();
      return new InstructionFutureValue<JSON>(variable(getSource(ctx),name));
	}

	@Override
	public InstructionFutureValue<JSON> visitId(IdContext ctx) {
		IdentContext ic = ctx.ident();
		String c;
		if (ic != null) {
			c = visitIdent(ic).string;
			IdContext idc = ctx.id();
			if (idc != null) {
				String a = visitId(idc).string;
				StringBuilder builder = new StringBuilder(a).append('.').append(c);
				return new InstructionFutureValue<>(builder.toString());
			}
			return new InstructionFutureValue<>(c);
		}
		return new InstructionFutureValue<>(visitId(ctx.id()).string); 
		
		/*
		String prefix = ctx.getChild(0).getText();
		String dd = visitId(ctx.id()).string; 
		StringBuilder builder = new StringBuilder(prefix).append(dd);
		return new InstructionFutureValue<>(builder.toString());
		*/
	}

	@Override
	public InstructionFutureValue<JSON> visitIdent(IdentContext ctx) {
		return new InstructionFutureValue<JSON>(ctx.getText());
	}

	@Override
	public InstructionFutureValue<JSON> visitNumber(NumberContext ctx) {
	   Number num = visitPnum(ctx.pnum()).number;
      int n = ctx.getChildCount();
      if(n>1) {
         if(num instanceof Long) {
            num = - (Long) num;
         } else {
            num = - (Double) num;
         }
      }
		return new InstructionFutureValue<JSON>(number(num,builder,getSource(ctx)));
	}
	
	

	@Override
	public InstructionFutureValue<JSON> visitJpath(JpathContext ctx) {
		return visitRe_expr(ctx.re_expr());
	}

	@Override
	public InstructionFutureValue<JSON> visitRe_expr(Re_exprContext ctx) {

		Tern_exprContext tc = ctx.tern_expr();
		if (tc != null)
			return visitTern_expr(tc);
		InstructionFutureValue<JSON> a = visitRe_expr(ctx.re_expr());
		String s = visitString(ctx.string()).string;
		return new InstructionFutureValue<>(reMatch(getSource(ctx),s, a.inst));
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
		return new InstructionFutureValue<>(conditional(visitTern_expr(tc).inst, visitValue(va).inst,
			visitValue(vb).inst,getSource(ctx)));
	}

	@Override
	public InstructionFutureValue<JSON> visitOr_expr(Or_exprContext ctx) {
		InstructionFutureValue<JSON> a = visitAnd_expr(ctx.and_expr());
		Or_exprContext c = ctx.or_expr();
		if (c != null) {
			final boolean or = ctx.getChild(1).getText().equals("or");
			return new InstructionFutureValue<JSON>(dyadic(getSource(ctx),visitOr_expr(c).inst,
				a.inst, new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON a, JSON b) {
						
						JSON result = or == a.isTrue() ? a : b;
						return result;
//						return or == a.isTrue() ? a : b;
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
			return new InstructionFutureValue<JSON>(dyadic(getSource(ctx),visitAnd_expr(c).inst,a.inst, 
				new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON a, JSON b) {
						if (a.getType() == JSONType.FRAME) {
							Frame newf = builder.frame((Frame) a);
							if (b.isTrue())
								((Frame) a).add(b);
						}
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
			return new InstructionFutureValue<JSON>(dyadic(getSource(ctx),visitEq_expr(c).inst,a.inst, 
				new DyadicAsyncFunction<JSON>() {
					@Override
					public JSON invoke(AsyncExecutionContext<JSON> eng, JSON a, JSON b) {
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
			return new InstructionFutureValue<JSON>(dyadic(getSource(ctx), visitRel_expr(c).inst, a.inst,df));
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
					return new InstructionFutureValue<JSON>(addInstruction(getSource(ctx),bv.inst,a.inst ));
				case "-":
					return new InstructionFutureValue<>(subInstruction(getSource(ctx),bv.inst,a.inst));
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
					return new InstructionFutureValue<>(mulInstruction(getSource(ctx),bv.inst,a.inst));
				case "div":
					return new InstructionFutureValue<>(divInstruction(getSource(ctx),bv.inst, a.inst));
				case "%":
					return new InstructionFutureValue<>(modInstruction(getSource(ctx),bv.inst, a.inst));
			}
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitUnary_expr(Unary_exprContext ctx) {
		Union_exprContext uc = ctx.union_expr();
		if (uc != null)
			return visitUnion_expr(uc);
		InstructionFutureValue<JSON> a = visitUnary_expr(ctx.unary_expr());
		return new InstructionFutureValue<JSON>(negate(getSource(ctx),a.inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitUnion_expr(Union_exprContext ctx) {
		List<Filter_pathContext> fps = ctx.filter_path();
		if (fps.size() == 1) {
			return visitFilter_path(fps.get(0));
		} else {
			final List<InstructionFuture<JSON>> seq = new ArrayList<>();
			for (Filter_pathContext fp : fps) {
				seq.add(visitFilter_path(fp).inst);
			}
			return new InstructionFutureValue<>(union(getSource(ctx),seq));
		}
	}

	@Override
	public InstructionFutureValue<JSON> visitFilter_path(Filter_pathContext ctx) {
		PathContext pc = ctx.path();
		if (pc != null)
			return visitPath(pc);
		InstructionFutureValue<JSON> a = visitFilter_path(ctx.filter_path());
		InstructionFutureValue<JSON> b = visitValue(ctx.value());
		return new InstructionFutureValue<>(dereference(getSource(ctx),a.inst, b.inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitPath(PathContext ctx) {
		Abs_pathContext ac = ctx.abs_path();
		return visitAbs_path(ac);
	}

	@Override
	public InstructionFutureValue<JSON> visitAbs_path(Abs_pathContext ctx) {
		final InstructionFutureValue<JSON> a = visitRel_path(ctx.rel_path());
		if (1 < ctx.getChildCount()) {
			return new InstructionFutureValue<>(abspath(getSource(ctx),a.inst));
		}
		return a;
	}

	@Override
	public InstructionFutureValue<JSON> visitRel_path(Rel_pathContext ctx) {
		final InstructionFutureValue<JSON> a = visitPathelement(ctx.pathelement());
		Rel_pathContext rp = ctx.rel_path();
		if (rp != null) {
			final InstructionFutureValue<JSON> c = visitRel_path(rp);
			return new InstructionFutureValue<JSON>(relpath(getSource(ctx),c.inst, a.inst));
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

		return new InstructionFutureValue<JSON>(dereference(getSource(ctx),pif.inst, vif.inst));
	}

	@Override
	public InstructionFutureValue<JSON> visitPathstep(PathstepContext ctx) {
		ObjectContext oc = ctx.object();
		if (oc != null) {
			return visitObject(oc);
		}
		ValueContext xvc = ctx.value();
		if(xvc!=null) return visitValue(xvc);

		ArrayContext ac = ctx.array();
		if (ac != null) {
			return visitArray(ac);
		}
		VariableContext vc = ctx.variable();
		if (vc != null)
			return visitVariable(vc);

		FuncContext fc = ctx.func();
		if (fc != null)
			return visitFunc(fc);
		
		FuncderefContext fdc = ctx.funcderef();
		if(fdc != null) return visitFuncderef(fdc);
		
		NumberContext nc = ctx.number();
		if (nc != null)
			return visitNumber(nc);

		StringContext sc = ctx.string();
		if (sc != null) {
			InstructionFutureValue<JSON> dd =  visitString(sc);
			dd.inst = value(dd.string,builder,getSource(ctx));
			return dd;
		}
/*
		JstringContext jc = ctx.jstring();
		if (jc != null)
			return visitJstring(jc);
*/
		RecursContext rc = ctx.recurs();
		if (rc != null)
			return visitRecurs(rc);

		IdContext ic = ctx.id();
		if (ic != null) {
			String id = visitId(ic).string;
			return new InstructionFutureValue<>(get(getSource(ctx),id));
		}

		String t = ctx.getText();
		{
			switch (t) {
//				case "*:*" :
//					return new InstructionFutureValue<>(mapChildren());
				case "true":
					return new InstructionFutureValue<>(value(true,builder,getSource(ctx)));
				case "false":
					return new InstructionFutureValue<>(value(false,builder,getSource(ctx)));
				case "null":
					return new InstructionFutureValue<>(value(builder,getSource(ctx)));
				case "*":
					return new InstructionFutureValue<>(stepChildren(getSource(ctx)));
				case ".":
					return new InstructionFutureValue<>(stepSelf(getSource(ctx)));
				case "..":
					return new InstructionFutureValue<>(stepParent(getSource(ctx)));

			}
		}
		throw new RuntimeException("token " + t + " is not implemented in pathstep");
	}

	@Override
	public InstructionFutureValue<JSON> visitRecurs(RecursContext ctx) {
		String t = ctx.getText();
		switch (t) {
			case "**":
				return new InstructionFutureValue<>(recursDown(getSource(ctx)));
			case "...":
				return new InstructionFutureValue<>(recursUp(getSource(ctx)));
		}
		throw new RuntimeException("token " + t + " is not implemented in recurs");
	}
/*
	@Override
	public InstructionFutureValue<JSON> visitJstring(JstringContext ctx) {
		StringContext sc = ctx.string();
		if (sc != null)
			return new InstructionFutureValue<JSON>(string(visitString(sc).string));
		StrcContext stc = ctx.strc();
		return visitStrc(stc);
	}

	@Override
	public InstructionFutureValue<JSON> visitStrc(StrcContext ctx) {
		JtlContext jc = ctx.jtl();
		if (jc != null) {
			return new InstructionFutureValue<>(tostr(visitJtl(jc).inst));
		}
		List<StrcContext> strc = ctx.strc();
		if (strc != null && strc.size() > 0) {
			List<InstructionFuture<JSON>> ii = new ArrayList<>(strc.size());
			for (StrcContext sc : strc) {
				ii.add(visitStrc(sc).inst);
			}
			return new InstructionFutureValue<>(strc(ii));
		}
		return new InstructionFutureValue<>(value(ctx.getText()));
	}
*/
   protected SourceInfo getSource(ParserRuleContext ctx,String name) {
      return JtlCompiler.getSource(source, ctx,name);
   }	
   protected SourceInfo getSource(ParserRuleContext ctx) {
      return JtlCompiler.getSource(source, ctx,null);
   }  
   
	@Override
	public InstructionFutureValue<JSON> visitPathindex(PathindexContext ctx) {
		return visitIndexlist(ctx.indexlist());
	}

	@Override
	public InstructionFutureValue<JSON> visitIndexlist(IndexlistContext ctx) {
		List<InstructionFuture<JSON>> elements = new ArrayList<>();
	//	List<IndexlContext> l= ctx.indexl();
		for(IndexlContext dxlc: ctx.indexl()) {
			elements.add(visitIndexl(dxlc).inst);
			
		}
		return new InstructionFutureValue<>(array(elements,getSource(ctx)));
	}

	@Override
	public InstructionFutureValue<JSON> visitIndexl(IndexlContext ctx) {
		List<ValueContext> cl = ctx.value();
		InstructionFuture<JSON> inst = visitValue(cl.get(0)).inst;
		if (cl.size() > 1) {
			List<InstructionFuture<JSON>> elements = new ArrayList<>();
			elements.add(inst);
			elements.add(visitValue(cl.get(1)).inst);
			return new InstructionFutureValue<>(array(elements,getSource(ctx)));
		} else {
			return new InstructionFutureValue<>(inst);
		}
	}

}
