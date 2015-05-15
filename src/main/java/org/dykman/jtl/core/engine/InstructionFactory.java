package org.dykman.jtl.core.engine;

import java.util.ArrayList;
import java.util.List;

import org.dykman.jtl.core.Duo;
import org.dykman.jtl.core.JSON;
import org.dykman.jtl.core.JSONArray;
import org.dykman.jtl.core.JSONException;
import org.dykman.jtl.core.JSONObject;
import org.dykman.jtl.core.JSONValue;
import org.dykman.jtl.core.parser.InstructionValue;

public class InstructionFactory {
	
	public static Instruction<JSON> variable(final String name) {
		return new AbstractInstruction<JSON>() {
			
			@Override
			public JSON call(Engine<JSON>eng,JSON t, List<JSON> args) {
				return eng.lookup(name);
			}
		};
	}

	public static Instruction<JSON> function(final String name,final List<Instruction<JSON>> iargs) {
		return new AbstractInstruction<JSON>() {
			
			@Override
			public JSON call(Engine<JSON>eng,JSON t, List<JSON> args) {
				List<JSON> a = new ArrayList<>();
				for(Instruction<JSON> i: iargs) {
					a.add(i.call(eng, null, null));
				}
				return eng.call(name, a);
			}
		};
	}

	public static Instruction<JSON> nil() {
		return new AbstractInstruction<JSON>() {
			
			@Override
			public JSON call(Engine<JSON>eng,JSON t, List<JSON> args) {
				return new JSONValue(t);
			}
		};
	}

	public static Instruction<JSON> bool(final Boolean b) {
		return new AbstractInstruction<JSON>() {
			
			@Override
			public JSON call(Engine<JSON>eng,JSON t, List<JSON> args) {
				return new JSONValue(t, b);
			}
		};
	}
	
	public static Instruction<JSON> string(final String str) {
		return new AbstractInstruction<JSON>() {
			
			@Override
			public JSON call(Engine<JSON>eng,JSON t, List<JSON> args) {
				return new JSONValue(t, str);
			}
		};
	}
	
	public static Instruction<JSON> decimal(final Double num) {
		return new AbstractInstruction<JSON>() {
			@Override
			public JSON call(Engine<JSON>eng,JSON t, List<JSON> args) {
				return new JSONValue(t, num);
			}
		};
	}
	public static Instruction<JSON> integer(final long num) {
		return new AbstractInstruction<JSON>() {
			@Override
			public JSON call(Engine<JSON>eng,JSON t, List<JSON> args) {
				return new JSONValue(t, num);
			}
		};
	}
	
	public static Instruction<JSON> array(final List<Instruction<JSON>> ch) {
		return new AbstractInstruction<JSON>() {
			@Override
			public JSON call(Engine<JSON> eng, JSON t, List<JSON> args) {
				JSONArray array= JSONArray.create(null,eng);
				array.setParent(t);
				for(Instruction<JSON> c:ch) {
					array.add(c.call(eng, array, null));
				}
				return array;
			}
		};
	}

	public static Instruction<JSON> pair(final JSONObject object,final Instruction<JSON>k,final Instruction<JSON>v) {
		return new AbstractInstruction<JSON>() {
			@Override
			public JSON call(Engine<JSON> eng, JSON t, List<JSON> args) {
				object.put(k.call(eng, t, null).toString(), v.call(eng, t, null));
				return object;
			}
		};
	}

	public static Instruction<JSON> object(final List<InstructionValue<JSON>> ll) {
		return new AbstractInstruction<JSON>() {
			@Override
			public JSON call(Engine<JSON> eng, JSON t, List<JSON> args) {
				try {
					JSONObject object;
					object = JSONObject.create(null, eng);
					object.setParent(t);
					for(InstructionValue<JSON> d : ll) {
						object.put(d.ninst.first, 
							d.ninst.second.call(eng, object, null));
					}
					return object;
				} catch (JSONException e) {
					return new JSONValue(null,"JSONException: " + e.getLocalizedMessage());
				}
			}
		};
	}
}
