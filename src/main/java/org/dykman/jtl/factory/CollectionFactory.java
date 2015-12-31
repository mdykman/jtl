package org.dykman.jtl.factory;

import java.util.Collection;

public interface CollectionFactory<T> {
	public Collection<T> createCollection();
	public Collection<T> createCollection(int cap);
	public Collection<T> copyCollection(Collection<T> c);
}