package org.dykman.jtl.core.engine;

import java.util.Map;

public interface MapFactory<T,U> {
	public Map<T,U> createMap();
	public Map<T,U> createMap(int c);
	public Map<T,U> copyMap(Map<T,U> rhs);
}
