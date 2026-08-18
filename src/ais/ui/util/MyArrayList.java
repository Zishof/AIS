package ais.ui.util;

import java.util.ArrayList;

public class MyArrayList<E> extends ArrayList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1111111111111111L;
	private int maxSize;

	public MyArrayList(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public boolean add(E e) {
		if (this.size() < maxSize && !contains(e)) {
			return super.add(e);
		}
		return false;
	}
}
