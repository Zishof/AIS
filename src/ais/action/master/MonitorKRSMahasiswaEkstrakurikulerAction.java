package ais.action.master;

import ais.database.model.Perkuliahan;

public class MonitorKRSMahasiswaEkstrakurikulerAction extends
		MonitorKRSMahasiswaAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -248128929411608561L;

	public MonitorKRSMahasiswaEkstrakurikulerAction() {
		ekstrakurikuler = Perkuliahan.EKSTRA;
	}
}
