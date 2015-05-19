package org.dykman.jtl.core.engine;

import java.util.Collection;

public interface CollectionFactory<T> {
	Collection<T> createCollection();
	Collection<T> createCollection(int cap);
}