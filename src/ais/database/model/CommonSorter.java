package ais.database.model;

import java.io.Serializable;

public class CommonSorter implements Serializable, Comparable<CommonSorter> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5763578573109731308L;

	private Serializable serializable;
	private Serializable serializable1;
	private Double value = 0.0;

	@Override
	public int compareTo(CommonSorter arg0) {
		return arg0.value.compareTo(value);
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Serializable getSerializable() {
		return serializable;
	}

	public void setSerializable(Serializable serializable) {
		this.serializable = serializable;
	}

	public Serializable getSerializable1() {
		return serializable1;
	}

	public void setSerializable1(Serializable serializable1) {
		this.serializable1 = serializable1;
	}

}
