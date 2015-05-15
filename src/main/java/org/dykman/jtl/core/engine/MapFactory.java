package org.dykman.jtl.core.engine;

import java.util.Map;

public interface MapFactory<T,U> {
	Map<T,U> createMap();
}
