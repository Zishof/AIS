package ais.action.master.penelitiandanpengabdian.helper;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;

public class PengajuanPengabdianHelper extends PengajuanPenelitianDanPengabdianHelper {

	public PengajuanPengabdianHelper() {
		super(false, ConstantValues.PENGABDIAN);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null) {
			usernamePengajuan = tbmuser.getUserId();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_DOSEN;
		} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			usernamePengajuan = tbmuser.getMahasiswa().getNim();
			diperuntukkanPengajuan = PengumumanAkademis.UNTUK_MAHASISWA;
		}
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Pengabdian Dosen";
	}

}
