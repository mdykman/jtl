package org.dykman.jtl.core.engine;

import java.util.Map;

public interface MapFactory<T,U> {
	Map<T,U> createMap();
	Map<T,U> createMap(int c);
}
