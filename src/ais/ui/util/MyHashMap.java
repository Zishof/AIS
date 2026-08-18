package ais.ui.util;

import java.util.HashMap;

public class MyHashMap<K, V> extends HashMap<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 12323333333333333L;
	private int MAX;

	public MyHashMap(int MAX) {
		this.MAX = MAX;
	}

	public V put(K key, V value) {
		if (this.size() < MAX) {
			return super.put(key, value);
		}
		return null;
	}

}
