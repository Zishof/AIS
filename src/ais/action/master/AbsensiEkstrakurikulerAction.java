package ais.action.master;

import ais.database.model.Perkuliahan;

public class AbsensiEkstrakurikulerAction extends AbsensiAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2058092290292136072L;

	public AbsensiEkstrakurikulerAction() {
		ekstrakurikuler = Perkuliahan.EKSTRA; 
	}

}
