package ais.database.model;

import ais.database.model.sop.DataSop;

public abstract class VoKunci extends DataSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract Tbmuser getDikunci();

	public abstract void setDikunci(Tbmuser dikunci);
}
