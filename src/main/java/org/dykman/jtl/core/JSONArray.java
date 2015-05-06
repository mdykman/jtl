package org.dykman.jtl.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class JSONArray extends AbstractJSON implements Iterable<JSON> {
	  ArrayList<JSON> arr = new ArrayList<>();
	  public void add(JSON j) {
		  arr.add(j);
	  }
	  public void addAll(Collection<JSON> j) {
		  arr.addAll(j);
	  }
	  public JSON get(int i) {
		  return arr.get(i);
	  }
	  public Iterator<JSON> iterator() {
		  return arr.iterator();
	  }
	  public String toString() {
		  StringBuilder builder = new StringBuilder("[");
		  boolean first = true;
		  for(JSON j: arr) {
			  if(first) first = false;
			  else builder.append(",");
			  builder.append(j.toString());
		  }
		  builder.append("]");
		  return builder.toString();
	  }

}
