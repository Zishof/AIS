package ais.database.model.sop;

import ais.database.model.GeneralValueObject;

public abstract class DataSop extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public abstract DisposisiSop getDisposisiSop();

	public abstract void setDisposisiSop(DisposisiSop disposisiSop);
}
