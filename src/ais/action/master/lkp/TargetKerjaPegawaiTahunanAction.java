package ais.action.master.lkp;

import ais.database.model.lkp.KegiatanTugasJabatan;

public class TargetKerjaPegawaiTahunanAction extends TargetKerjaPegawaiAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TargetKerjaPegawaiTahunanAction() {
		this(KegiatanTugasJabatan.TAHUNAN);
	}

	public TargetKerjaPegawaiTahunanAction(String periode) {
		super(periode);

	}

}
