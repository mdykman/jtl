package org.dykman.jtl.core.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dykman.jtl.core.JSON;

public class ConcurrentMapFactory implements MapFactory {

	@Override
	public Map<String, JSON> createMap() {
		return new ConcurrentHashMap<String, JSON>();
	}

}
