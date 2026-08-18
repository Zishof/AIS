package ais.action.master.lkp;

import ais.database.model.lkp.KegiatanTugasJabatan;

public class RealisasiKerjaPegawaiTahunanAction extends RealisasiKerjaPegawaiAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RealisasiKerjaPegawaiTahunanAction() {
		super(KegiatanTugasJabatan.TAHUNAN);
		// TODO Auto-generated constructor stub
	}

	public RealisasiKerjaPegawaiTahunanAction(String periode) {
		super(periode);
	}

}
