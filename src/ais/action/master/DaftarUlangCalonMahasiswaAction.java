package ais.action.master;

import org.zkoss.zk.ui.Component;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.JenisKegiatan;

public class DaftarUlangCalonMahasiswaAction extends DaftarUlangMahasiswaBaruAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1035398565522440476L;

	public DaftarUlangCalonMahasiswaAction() {
		super();
		super.jenisKegiatan = (JenisKegiatan) ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.selectComboItem(semesterPilihan, 0);
		if (semesterPilihan != null) { semesterPilihan.setDisabled(true); }
	}

}
